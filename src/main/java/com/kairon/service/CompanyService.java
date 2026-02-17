package com.kairon.service;

import com.kairon.domain.entity.Company;
import com.kairon.domain.entity.User;
import com.kairon.dto.request.CompanyUpdateRequest;
import com.kairon.dto.response.CompanyResponse;
import com.kairon.dto.response.PublicCompanyResponse;
import com.kairon.exception.BusinessException;
import com.kairon.exception.EntityNotFoundException;
import com.kairon.mapper.CompanyMapper;
import com.kairon.repository.CompanyRepository;
import com.kairon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService extends BaseService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final CompanyMapper companyMapper;
    private final FileStorageService fileStorageService;

    /* =======================
       MÉTODOS POR ID (USADOS PELO FRONTEND ATUAL)
    ======================= */

    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(String id) {
        // Usa o mapeamento manual para garantir que todos os campos retornem
        return mapToResponse(getCompanyEntity(id));
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompanyPublic(String id) {
        return mapToResponse(getCompanyEntity(id));
    }

    @Transactional
    public CompanyResponse update(String id, CompanyUpdateRequest request) { // Nome padronizado para 'update'
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Company not found"));

        // 🔍 DEBUG: Verificando o que chegou do celular
        System.out.println(">>> UPDATE INICIADO PARA EMPRESA: " + id);
        System.out.println(">>> Template Recebido: " + request.getWhatsappTemplate());

        // Atualização de Dados Básicos
        if (request.getName() != null) company.setName(request.getName());
        if (request.getPhone() != null) company.setPhone(request.getPhone());
        if (request.getEmail() != null) company.setEmail(request.getEmail());
        if (request.getDescription() != null) company.setDescription(request.getDescription());
        if (request.getWebsite() != null) company.setWebsite(request.getWebsite());

        // 👇 AQUI ESTÁ A ATUALIZAÇÃO DO TEMPLATE
        if (request.getWhatsappTemplate() != null) {
            company.setWhatsappTemplate(request.getWhatsappTemplate());
        }

        // Endereço
        if (request.getAddress() != null) company.setAddress(request.getAddress());
        if (request.getCity() != null) company.setCity(request.getCity());
        if (request.getState() != null) company.setState(request.getState());
        if (request.getZipCode() != null) company.setZipCode(request.getZipCode());

        // Campos JSON (Settings e Horários)
        if (request.getSettings() != null) {
            company.setSettings(request.getSettings());
        }

        if (request.getBusinessHours() != null) {
            company.setBusinessHours(request.getBusinessHours());
        }

        // Campos Extras
        if (request.getTimezone() != null) company.setTimezone(request.getTimezone());
        if (request.getCurrency() != null) company.setCurrency(request.getCurrency());
        if (request.getBusinessType() != null) company.setBusinessType(request.getBusinessType());
        if (request.getSlotDuration() != null) company.setSlotDuration(request.getSlotDuration()); // Adicionado
        if (request.getBufferTime() != null) company.setBufferTime(request.getBufferTime());     // Adicionado

        Company savedCompany = companyRepository.save(company);

        System.out.println(">>> EMPRESA SALVA COM SUCESSO!");

        return mapToResponse(savedCompany);
    }

    @Transactional
    public CompanyResponse uploadLogo(String id, MultipartFile file) {
        Company company = getCompanyEntity(id);

        // Usa o FileStorageService para salvar o arquivo
        String logoUrl = fileStorageService.uploadFile(file);

        // Salva a URL no banco
        company.setLogoUrl(logoUrl);

        Company savedCompany = companyRepository.save(company);
        return mapToResponse(savedCompany);
    }

    /* =======================
       HELPER DE MAPEAMENTO MANUAL
       (Garante que os dados novos não se percam no retorno)
    ======================= */
    private CompanyResponse mapToResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId()) // Importante: CompanyResponse espera 'id' ou 'companyId'? Ajuste conforme seu DTO
                // Se seu DTO CompanyResponse tiver o campo 'companyId', use .companyId(company.getId())

                .name(company.getName())
                .email(company.getEmail())
                .phone(company.getPhone())
                .description(company.getDescription())
                .website(company.getWebsite())
                .logoUrl(company.getLogoUrl())
                .whatsappTemplate(company.getWhatsappTemplate())// 🔥 GARANTE O LOGO

                .address(company.getAddress())
                .city(company.getCity())
                .state(company.getState())
                .zipCode(company.getZipCode())

                .businessHours(company.getBusinessHours()) // 🔥 GARANTE OS HORÁRIOS
                .settings(company.getSettings())

                .businessType(company.getBusinessType())
                .currency(company.getCurrency())
                .isActive(company.isActive())
                .build();
    }

    /* =======================
       MÉTODOS LEGADOS / PÚBLICOS
    ======================= */

    @Transactional(readOnly = true)
    public CompanyResponse getCompanyData() {
        String email = getCurrentUserEmail();
        User user = userRepository.findActiveUserByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        Company company = companyRepository.findActiveById(user.getCompany().getId())
                .orElseThrow(() -> new BusinessException("Company not found"));

        return mapToResponse(company);
    }

    @Transactional(readOnly = true)
    public PublicCompanyResponse getPublicCompanyData(String slug) {
        Company company = companyRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException("Company not found"));

        if (!company.isActive()) {
            throw new BusinessException("Company is not active");
        }
        return companyMapper.toPublicResponse(company);
    }

    /* =======================
       HELPER INTERNO
    ======================= */

    public Company getCompanyEntity(String companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company not found with ID: " + companyId));
    }
}