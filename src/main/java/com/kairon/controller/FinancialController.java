package com.kairon.controller;

import com.kairon.domain.entity.FinancialRecord;
import com.kairon.domain.entity.User;
import com.kairon.domain.entity.Professional;
import com.kairon.dto.request.DashboardRequest;
import com.kairon.dto.request.FinancialRequest;
import com.kairon.dto.response.CategorySumResponse;
import com.kairon.dto.response.DashboardResponse;
import com.kairon.dto.response.FinancialResponse;
import com.kairon.dto.response.MonthlyHistoryResponse; // 👈 NOVO IMPORT AQUI!
import com.kairon.repository.FinancialRecordRepository;
import com.kairon.repository.ProfessionalRepository;
import com.kairon.repository.UserRepository;
import com.kairon.security.util.SecurityUtils;
import com.kairon.service.FinancialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/financial") // 👈 Ajustado para bater com o front (era /finance)
@RequiredArgsConstructor
@Tag(name = "Financial", description = "Financial management endpoints")
@PreAuthorize("hasRole('OWNER') or hasRole('PROFESSIONAL')")
public class FinancialController {

    private final FinancialService financialService;

    // Injeção direta para garantir a busca correta das Vendas
    private final FinancialRecordRepository financialRecordRepository;
    private final UserRepository userRepository;
    private final ProfessionalRepository professionalRepository;

    // NOVO MÉTODO NO CONTROLLER
    @GetMapping
    @Operation(summary = "Get all financial records (Sales + Services)")
    public ResponseEntity<List<FinancialResponse>> getAllRecords(Principal principal) {

        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        String companyId = user.getCompany().getId();

        // CHAMA O SERVICE QUE JUNTA TUDO (HÍBRIDO)
        List<FinancialResponse> allMovements = financialService.getAllMovements(companyId);

        // Se for profissional, filtra aqui ou deixa o service filtrar (o service atual traz tudo da empresa)
        // Se quiser filtrar por profissional na listagem também:
        if (user.getRole().name().equals("PROFESSIONAL")) {
            Professional prof = professionalRepository.findByUserId(user.getId()).orElse(null);
            if (prof != null) {
                allMovements = allMovements.stream()
                        .filter(m -> m.getProfessionalName() != null && m.getProfessionalName().equals(prof.getName()))
                        .collect(Collectors.toList());
            }
        }

        return ResponseEntity.ok(allMovements);
    }

    // 👇 NOVA ROTA PARA A TABELA DE HISTÓRICO MENSAL (SWIPE) 👇
    @GetMapping("/monthly-history")
    @Operation(summary = "Get detailed monthly history for the data grid (PLUS Only)")
    public ResponseEntity<List<MonthlyHistoryResponse>> getMonthlyHistory() {
        String companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(financialService.getMonthlyHistory(companyId));
    }

    // --- MÉTODOS ANTIGOS MANTIDOS (Atualizados para usar /financial) ---

    @GetMapping("/revenue-chart")
    public ResponseEntity<List<DashboardResponse.DailySummary>> getRevenueChart() {
        String companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(financialService.getRevenueChartData(companyId));
    }

    @GetMapping("/records/{recordId}")
    public ResponseEntity<FinancialResponse> getFinancialRecordById(@PathVariable String recordId) {
        String companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(financialService.getFinancialRecordById(companyId, recordId));
    }

    @PostMapping("/records")
    public ResponseEntity<FinancialResponse> createFinancialRecord(@Valid @RequestBody FinancialRequest request) {
        String companyId = SecurityUtils.getCurrentCompanyId();
        FinancialResponse record = financialService.createFinancialRecord(companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    @PutMapping("/records/{recordId}")
    public ResponseEntity<FinancialResponse> updateFinancialRecord(@PathVariable String recordId, @Valid @RequestBody FinancialRequest request) {
        String companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(financialService.updateFinancialRecord(companyId, recordId, request));
    }

    @DeleteMapping("/records/{recordId}") // Mantivemos o /records aqui pois o front deleta por ID
    public ResponseEntity<Void> deleteFinancialRecord(@PathVariable String recordId) {
        String companyId = SecurityUtils.getCurrentCompanyId();
        financialService.deleteFinancialRecord(companyId, recordId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(@Valid @ModelAttribute com.kairon.dto.request.DashboardRequest request) {
        String companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(financialService.getDashboard(companyId, request));
    }

    // --- MAPPER MANUAL (Para garantir que Vendas de Produtos apareçam com o nome certo) ---
    private FinancialResponse mapToResponse(FinancialRecord r) {
        return FinancialResponse.builder()
                .id(r.getId())
                .type(r.getType())
                // .category(r.getCategory()) // Se ainda der erro aqui, certifique-se que adicionou o campo no DTO
                .amount(r.getAmount() != null ? r.getAmount().doubleValue() : 0.0) // 👈 CORREÇÃO AQUI: Converte BigDecimal para Double
                .description(r.getDescription())
                .referenceDate(r.getReferenceDate())
                .paymentMethod(r.getPaymentMethod())
                .status(r.getStatus())
                .professionalName(r.getProfessional() != null ? r.getProfessional().getName() : null)
                .clientName(r.getAppointment() != null ? r.getAppointment().getClient().getName() : "Cliente Balcão")
                .build();
    }


    @GetMapping("/weekly-history")
    public ResponseEntity<List<MonthlyHistoryResponse>> getWeeklyHistory() {
        return ResponseEntity.ok(financialService.getWeeklyHistory(SecurityUtils.getCurrentCompanyId()));
    }


    @GetMapping("/expenses-chart")
    public ResponseEntity<List<CategorySumResponse>> getExpensesChart(
            @Valid @ModelAttribute DashboardRequest request) {

        String companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(financialService.getExpensesChartData(companyId, request));
    }
}