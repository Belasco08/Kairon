package com.kairon.service;

import com.kairon.component.PlanGuard; // Import do Guardião
import com.kairon.domain.entity.*;
import com.kairon.domain.enums.FinancialType;
import com.kairon.dto.request.SellProductRequest;
import com.kairon.repository.CompanyRepository;
import com.kairon.repository.ProductRepository;
import com.kairon.repository.FinancialRecordRepository;
import com.kairon.repository.ProfessionalRepository;
import com.kairon.dto.request.ProductRequest;
import com.kairon.dto.response.ProductResponse;
import com.kairon.exception.BusinessException;
import com.kairon.repository.UserRepository;

// 👇 IMPORT CORRIGIDO: Usando o do Spring para habilitar o readOnly=true
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final FinancialRecordRepository financialRecordRepository;
    private final UserRepository userRepository;
    private final ProfessionalRepository professionalRepository;
    private final PlanGuard planGuard; // Injetado para verificar o plano

    // 1. CRIAR
    @Transactional
    public ProductResponse create(ProductRequest request, String userEmail) {

        // --- TRAVA FREEMIUM: ESTOQUE ---
        // Apenas usuários PLUS podem cadastrar e controlar estoque.
        planGuard.checkPlusAccess(request.getCompanyId());

        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new BusinessException("Empresa não encontrada"));

        Product product = Product.builder()
                .name(request.getName())
                .barcode(request.getBarcode())
                .type(request.getType())
                .costPrice(request.getCostPrice())
                .salePrice(request.getSalePrice())
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                .minStockLevel(request.getMinStockLevel() != null ? request.getMinStockLevel() : 5)
                .photoUrl(request.getPhotoUrl())
                .company(company)
                .isActive(true)
                .build();

        Product savedProduct = productRepository.save(product);

        // Despesa Inicial (Só gera se tiver custo e estoque > 0)
        if (savedProduct.getStockQuantity() > 0 && savedProduct.getCostPrice() != null) {
            BigDecimal totalCost = savedProduct.getCostPrice().multiply(BigDecimal.valueOf(savedProduct.getStockQuantity()));
            createStockExpense(savedProduct, company, userEmail, "Estoque Inicial", totalCost);
        }

        return mapToResponse(savedProduct);
    }

    // 👇 SOLUÇÃO APLICADA: Transação apenas de leitura para o Postgres não barrar a foto
    @Transactional(readOnly = true)
    public List<ProductResponse> listByCompany(String companyId) {
        // Listagem geralmente é liberada para visualização, mas a criação é travada.
        return productRepository.findByCompanyIdAndIsActiveTrue(companyId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // 2. ATUALIZAR (REPOSIÇÃO)
    @Transactional
    public ProductResponse update(String id, ProductRequest request, String userEmail) {
        // Se a criação é travada, a atualização também deve ser (para garantir consistência)
        // Mas como só Plus consegue criar, teoricamente só Plus vai chegar aqui.
        // Por segurança, validamos de novo.
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Produto não encontrado"));

        planGuard.checkPlusAccess(product.getCompany().getId());

        int oldStock = product.getStockQuantity();

        // Atualiza dados
        product.setName(request.getName());
        product.setBarcode(request.getBarcode());
        product.setType(request.getType());
        product.setCostPrice(request.getCostPrice());
        product.setSalePrice(request.getSalePrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setMinStockLevel(request.getMinStockLevel());
        product.setPhotoUrl(request.getPhotoUrl());

        Product savedProduct = productRepository.save(product);

        // LÓGICA DE REPOSIÇÃO (Se aumentou o estoque)
        if (request.getStockQuantity() > oldStock) {
            int addedQuantity = request.getStockQuantity() - oldStock;

            BigDecimal costPrice = request.getCostPrice() != null ? request.getCostPrice() : product.getCostPrice();

            if (costPrice != null) {
                BigDecimal cost = costPrice.multiply(BigDecimal.valueOf(addedQuantity));
                if (cost.compareTo(BigDecimal.ZERO) > 0) {
                    createStockExpense(savedProduct, product.getCompany(), userEmail, "Reposição (" + addedQuantity + "un)", cost);
                }
            }
        }

        return mapToResponse(savedProduct);
    }

    @Transactional
    public void delete(String id) {
        if (!productRepository.existsById(id)) {
            throw new BusinessException("Produto não encontrado");
        }
        productRepository.deleteById(id);
    }

    // 3. VENDER
    @Transactional
    public void sell(String id, SellProductRequest request, String userEmail) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Produto não encontrado"));

        // Venda também exige Plus, pois mexe no estoque
        planGuard.checkPlusAccess(product.getCompany().getId());

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new BusinessException("Estoque insuficiente! Disponível: " + product.getStockQuantity());
        }

        // Baixa estoque
        product.setStockQuantity(product.getStockQuantity() - request.getQuantity());
        productRepository.save(product);

        // Financeiro
        BigDecimal price = product.getSalePrice();
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalAmount = price.multiply(BigDecimal.valueOf(request.getQuantity()));

            Professional professional = getOrCreateProfessional(userEmail);

            FinancialRecord income = FinancialRecord.builder()
                    .title("Venda: " + product.getName())
                    .description("Qtd: " + request.getQuantity() + " | Cliente: " + (request.getClientName() != null ? request.getClientName() : "Balcão"))
                    .amount(totalAmount)
                    .type(FinancialType.INCOME)
                    .category("VENDAS_PRODUTOS")
                    .status("PAID")
                    .paymentMethod(request.getPaymentMethod())
                    .referenceDate(LocalDateTime.now())
                    .company(product.getCompany())
                    .professional(professional)
                    .build();

            financialRecordRepository.save(income);
        }
    }

    // --- Helpers ---

    private Professional getOrCreateProfessional(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado: " + userEmail));

        return professionalRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Professional newProf = new Professional();
                    newProf.setName(user.getName() + " (Admin)");
                    newProf.setPhone(user.getPhone());
                    newProf.setUser(user);
                    newProf.setCompany(user.getCompany());
                    newProf.setActive(true);
                    newProf.setCommissionPercentage(BigDecimal.ZERO);
                    return professionalRepository.save(newProf);
                });
    }

    private void createStockExpense(Product product, Company company, String userEmail, String reason, BigDecimal amount) {
        Professional professional = getOrCreateProfessional(userEmail);

        String category = "ESTOQUE_INTERNO";
        if (product.getType() != null && product.getType().name().equals("RESALE")) {
            category = "ESTOQUE_VENDA";
        }

        FinancialRecord expense = FinancialRecord.builder()
                .title(reason + ": " + product.getName())
                .description("Ajuste de estoque do produto " + product.getName())
                .amount(amount)
                .type(FinancialType.EXPENSE)
                .category(category)
                .status("PAID")
                .paymentMethod("CAIXA")
                .referenceDate(LocalDateTime.now())
                .company(company)
                .professional(professional)
                .build();

        financialRecordRepository.save(expense);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .barcode(product.getBarcode())
                .type(product.getType())
                .costPrice(product.getCostPrice())
                .salePrice(product.getSalePrice())
                .stockQuantity(product.getStockQuantity())
                .minStockLevel(product.getMinStockLevel())
                .photoUrl(product.getPhotoUrl())
                .build();
    }
}