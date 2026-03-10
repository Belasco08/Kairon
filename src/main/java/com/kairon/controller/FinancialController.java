package com.kairon.controller;

import com.kairon.domain.entity.FinancialRecord;
import com.kairon.domain.entity.User;
import com.kairon.domain.entity.Professional;
import com.kairon.dto.request.DashboardRequest;
import com.kairon.dto.request.FinancialRequest;
import com.kairon.dto.response.CategorySumResponse;
import com.kairon.dto.response.DashboardResponse;
import com.kairon.dto.response.FinancialResponse;
import com.kairon.dto.response.MonthlyHistoryResponse;
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
@RequestMapping("/financial")
@RequiredArgsConstructor
@Tag(name = "Financial", description = "Financial management endpoints")
@PreAuthorize("hasRole('OWNER') or hasRole('PROFESSIONAL')")
public class FinancialController {

    private final FinancialService financialService;
    private final FinancialRecordRepository financialRecordRepository;
    private final UserRepository userRepository;
    private final ProfessionalRepository professionalRepository;

    /* =========================
       MÉTODOS DE BUSCA (GET)
       ========================= */

    @GetMapping
    @Operation(summary = "Get all financial records (Sales + Services)")
    public ResponseEntity<List<FinancialResponse>> getAllRecords(Principal principal) {
        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        String companyId = user.getCompany().getId();

        List<FinancialResponse> allMovements = financialService.getAllMovements(companyId);

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

    @GetMapping("/records/{recordId}")
    public ResponseEntity<FinancialResponse> getFinancialRecordById(@PathVariable String recordId) {
        String companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(financialService.getFinancialRecordById(companyId, recordId));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(@Valid @ModelAttribute DashboardRequest request) {
        String companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(financialService.getDashboard(companyId, request));
    }

    @GetMapping("/revenue-chart")
    public ResponseEntity<List<DashboardResponse.DailySummary>> getRevenueChart() {
        String companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(financialService.getRevenueChartData(companyId));
    }

    @GetMapping("/expenses-chart")
    public ResponseEntity<List<CategorySumResponse>> getExpensesChart(@Valid @ModelAttribute DashboardRequest request) {
        String companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(financialService.getExpensesChartData(companyId, request));
    }

    @GetMapping("/weekly-history")
    public ResponseEntity<List<MonthlyHistoryResponse>> getWeeklyHistory() {
        return ResponseEntity.ok(financialService.getWeeklyHistory(SecurityUtils.getCurrentCompanyId()));
    }

    @GetMapping("/monthly-history")
    @Operation(summary = "Get detailed monthly history for the data grid (PLUS Only)")
    public ResponseEntity<List<MonthlyHistoryResponse>> getMonthlyHistory() {
        String companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(financialService.getMonthlyHistory(companyId));
    }

    /* =========================
       LANÇAMENTOS (CRIAR, ATUALIZAR E DELETAR)
       ========================= */

    @PostMapping("/records")
    @Operation(summary = "Criar um novo lançamento financeiro (Receita/Despesa)")
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

    @DeleteMapping("/records/{recordId}")
    public ResponseEntity<Void> deleteFinancialRecord(@PathVariable String recordId) {
        String companyId = SecurityUtils.getCurrentCompanyId();
        financialService.deleteFinancialRecord(companyId, recordId);
        return ResponseEntity.noContent().build();
    }

    /* =========================
       BAIXA DE PAGAMENTO (AÇÃO)
       ========================= */

    @PostMapping("/records/{id}/pay")
    @Operation(summary = "Dar baixa em um registro financeiro pendente")
    public ResponseEntity<Void> payRecord(@PathVariable String id) {
        String companyId = SecurityUtils.getCurrentCompanyId();
        financialService.payRecord(companyId, id);
        return ResponseEntity.ok().build();
    }

    /* =========================
       ACERTO DE CONTAS DA EQUIPE (FECHAMENTO)
       ========================= */

    @GetMapping("/settlement/{professionalId}")
    public ResponseEntity<com.kairon.dto.response.SettlementResponse> getSettlement(@PathVariable String professionalId) {
        String companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(financialService.getProfessionalSettlement(companyId, professionalId));
    }

    @PostMapping("/settlement/{professionalId}/pay")
    public ResponseEntity<Void> paySettlement(@PathVariable String professionalId) {
        String companyId = SecurityUtils.getCurrentCompanyId();
        financialService.payProfessionalSettlement(companyId, professionalId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/settlement/{professionalId}/advance")
    public ResponseEntity<Void> addAdvance(
            @PathVariable String professionalId,
            @RequestParam Double amount,
            @RequestParam(required = false) String description) {
        String companyId = SecurityUtils.getCurrentCompanyId();
        financialService.addProfessionalAdvance(companyId, professionalId, amount, description);
        return ResponseEntity.ok().build();
    }
}