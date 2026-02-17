package com.kairon.service;

import com.kairon.component.PlanGuard;
import com.kairon.domain.entity.*;
import com.kairon.domain.enums.AppointmentStatus;
import com.kairon.domain.enums.FinancialType;
import com.kairon.domain.enums.Role;
import com.kairon.dto.request.DashboardRequest;
import com.kairon.dto.request.FinancialFilterRequest;
import com.kairon.dto.request.FinancialRequest;
import com.kairon.dto.response.DashboardResponse;
import com.kairon.dto.response.FinancialResponse;
import com.kairon.exception.BusinessException;
import com.kairon.repository.*;
import com.kairon.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialService {

    private final FinancialRecordRepository financialRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PlanGuard planGuard;

    /* =========================
       LISTAGEM UNIFICADA (EXTRATO)
       ========================= */

    @Transactional(readOnly = true)
    public List<FinancialResponse> getAllMovements(String companyId) {
        List<FinancialRecord> records = financialRecordRepository.findByCompanyIdOrderByReferenceDateDesc(companyId);
        List<Appointment> appointments = appointmentRepository.findByCompanyIdAndStatusOrderByStartTimeDesc(companyId, AppointmentStatus.COMPLETED);

        List<FinancialResponse> appointmentResponses = appointments.stream()
                .map(this::mapAppointmentToResponse).collect(Collectors.toList());

        List<FinancialResponse> recordResponses = records.stream()
                .map(this::mapRecordToResponse).collect(Collectors.toList());

        return Stream.concat(recordResponses.stream(), appointmentResponses.stream())
                .sorted(Comparator.comparing(FinancialResponse::getReferenceDate).reversed())
                .collect(Collectors.toList());
    }

    /* =========================
       DASHBOARD (SOMA TUDO)
       ========================= */

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String companyId, DashboardRequest request) {

        // --- TRAVA FREEMIUM CORRIGIDA ---
        // Permite "day" e "month" para contas Free.
        // Bloqueia "week", "year" e datas retroativas manuais.
        if (!planGuard.isPlus(companyId)) {
            String requestedPeriod = request.getPeriod() != null ? request.getPeriod().toLowerCase() : "month";

            // Se tentar burlar pedindo semana ou ano, forçamos para o mês atual
            if (!requestedPeriod.equals("day") && !requestedPeriod.equals("month")) {
                request.setPeriod("month");
            }

            // Remove datas customizadas para evitar buscar histórico passado sem pagar
            request.setStartDate(null);
            request.setEndDate(null);
        }

        // 1. LÓGICA DE DATAS
        LocalDateTime startDate;
        LocalDateTime endDate = request.getEndDate() != null
                ? request.getEndDate().with(LocalTime.MAX)
                : LocalDate.now().atTime(LocalTime.MAX);

        String period = request.getPeriod() != null ? request.getPeriod().toLowerCase() : "month";

        if (request.getStartDate() != null) {
            startDate = request.getStartDate().toLocalDate().atStartOfDay();
        } else {
            switch (period) {
                case "day":
                    startDate = LocalDate.now().atStartOfDay();
                    break;
                case "week":
                    startDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
                    break;
                case "year":
                    startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();
                    break;
                case "month":
                default:
                    startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
                    break;
            }
        }

        // 2. BUSCAS
        List<FinancialRecord> financialRecords = financialRecordRepository.findByCompanyIdAndReferenceDateBetween(companyId, startDate, endDate);
        List<Appointment> appointments = appointmentRepository.findByCompanyIdAndStartTimeBetweenAndStatus(companyId, startDate, endDate, AppointmentStatus.COMPLETED);

        // 3. FILTRO PROFISSIONAL
        String currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow();

        if (currentUser.getRole() == Role.PROFESSIONAL) {
            Professional prof = professionalRepository.findByUserId(currentUserId).orElseThrow();
            financialRecords = financialRecords.stream().filter(r -> r.getProfessional() != null && r.getProfessional().getId().equals(prof.getId())).collect(Collectors.toList());
            appointments = appointments.stream().filter(a -> a.getProfessional() != null && a.getProfessional().getId().equals(prof.getId())).collect(Collectors.toList());
        }

        // 4. SOMAS
        BigDecimal recordsRevenue = financialRecords.stream().filter(r -> r.getType() == FinancialType.INCOME).map(FinancialRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal servicesRevenue = appointments.stream().map(Appointment::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = financialRecords.stream().filter(r -> r.getType() == FinancialType.EXPENSE).map(FinancialRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = recordsRevenue.add(servicesRevenue);
        BigDecimal balance = totalRevenue.subtract(totalExpenses);
        long totalCount = appointments.size() + financialRecords.stream().filter(r -> r.getType() == FinancialType.INCOME).count();

        BigDecimal averageTicket = totalCount > 0 ? totalRevenue.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // 5. GRÁFICOS
        List<DashboardResponse.DailySummary> dailyEvolution = calculateCombinedDailyEvolution(financialRecords, appointments, startDate, endDate);
        List<DashboardResponse.ServiceSummary> topServices = calculateCombinedTopServices(financialRecords, appointments);

        return DashboardResponse.builder()
                .revenue(totalRevenue.doubleValue())
                .expenses(totalExpenses.doubleValue())
                .balance(balance.doubleValue())
                .appointmentCount((int) appointments.size())
                .averageTicket(averageTicket.doubleValue())
                .dailyEvolution(dailyEvolution)
                .topServices(topServices)
                .busyHours(new ArrayList<>())
                .build();
    }
    // --- GRÁFICO DE DESPESAS INTELIGENTE (PLUS ONLY) ---

    @Transactional(readOnly = true)
    public List<com.kairon.dto.response.CategorySumResponse> getExpensesChartData(String companyId, DashboardRequest request) {

        // --- TRAVA FREEMIUM: BLOQUEIO TOTAL ---
        // Apenas usuários PLUS podem ver o detalhamento de onde gastam o dinheiro.
        planGuard.checkPlusAccess(companyId);

        // Lógica de Datas
        LocalDateTime startDate;
        LocalDateTime endDate = request.getEndDate() != null ? request.getEndDate().with(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);
        String period = request.getPeriod() != null ? request.getPeriod().toLowerCase() : "month";

        if (request.getStartDate() != null) {
            startDate = request.getStartDate().toLocalDate().atStartOfDay();
        } else {
            switch (period) {
                case "day": startDate = LocalDate.now().atStartOfDay(); break;
                case "week": startDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(); break;
                case "year": startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfYear()).atStartOfDay(); break;
                case "month": default: startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(); break;
            }
        }

        List<FinancialRecord> expenses = financialRecordRepository.findByCompanyIdAndReferenceDateBetween(companyId, startDate, endDate)
                .stream().filter(r -> r.getType() == FinancialType.EXPENSE).collect(Collectors.toList());

        Map<String, BigDecimal> groupedMap = new HashMap<>();

        for (FinancialRecord r : expenses) {
            String rawCat = r.getCategory() != null ? r.getCategory().toUpperCase() : "OUTROS";
            String label;

            if (rawCat.contains("ESTOQUE") || rawCat.equals("REPOSICAO_ESTOQUE")) {
                if (rawCat.equals("ESTOQUE_VENDA")) label = "Estoque (Vendas)";
                else if (rawCat.equals("ESTOQUE_INTERNO")) label = "Estoque (Interno)";
                else label = "Estoque Geral";
            } else if (rawCat.equals("PAGAMENTO_FUNCIONARIO") || rawCat.equals("COMISSAO") || rawCat.equals("SALARIO")) {
                label = "Pagamento Equipe";
            } else if (rawCat.equals("ALUGUEL") || rawCat.equals("CONTA")) {
                label = "Custos Fixos";
            } else if (rawCat.equals("OUTROS")) {
                label = "Pagamentos Diversos";
            } else {
                label = rawCat.replace("_", " ").toLowerCase();
                label = Character.toUpperCase(label.charAt(0)) + label.substring(1);
            }

            groupedMap.put(label, groupedMap.getOrDefault(label, BigDecimal.ZERO).add(r.getAmount()));
        }

        return groupedMap.entrySet().stream()
                .map(e -> new com.kairon.dto.response.CategorySumResponse(e.getKey(), e.getValue()))
                .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                .collect(Collectors.toList());
    }

    // --- HELPERS E MAPPERS ---

    private List<DashboardResponse.DailySummary> calculateCombinedDailyEvolution(List<FinancialRecord> records, List<Appointment> appointments, LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        Map<LocalDate, BigDecimal> dailyMap = new HashMap<>();

        records.stream().filter(r -> r.getType() == FinancialType.INCOME).forEach(r -> {
            LocalDate d = r.getReferenceDate().toLocalDate();
            dailyMap.put(d, dailyMap.getOrDefault(d, BigDecimal.ZERO).add(r.getAmount()));
        });

        appointments.forEach(a -> {
            LocalDate d = a.getStartTime().toLocalDate();
            dailyMap.put(d, dailyMap.getOrDefault(d, BigDecimal.ZERO).add(a.getTotalPrice()));
        });

        List<DashboardResponse.DailySummary> evolution = new ArrayList<>();
        LocalDate current = start.toLocalDate();
        while (!current.isAfter(end.toLocalDate()) && !current.isAfter(LocalDate.now())) {
            evolution.add(DashboardResponse.DailySummary.builder().date(current.format(fmt)).revenue(dailyMap.getOrDefault(current, BigDecimal.ZERO).doubleValue()).appointments(0).build());
            current = current.plusDays(1);
        }
        return evolution;
    }

    private List<DashboardResponse.ServiceSummary> calculateCombinedTopServices(List<FinancialRecord> records, List<Appointment> appointments) {
        Map<String, Double> itemMap = new HashMap<>();
        Map<String, Integer> countMap = new HashMap<>();

        for (Appointment a : appointments) {
            String name = (a.getAppointmentServices() != null && !a.getAppointmentServices().isEmpty())
                    ? a.getAppointmentServices().iterator().next().getName() : "Serviço";
            itemMap.put(name, itemMap.getOrDefault(name, 0.0) + a.getTotalPrice().doubleValue());
            countMap.put(name, countMap.getOrDefault(name, 0) + 1);
        }

        for (FinancialRecord r : records) {
            if (r.getType() == FinancialType.INCOME) {
                String name = r.getTitle() != null ? r.getTitle() : (r.getCategory() != null ? r.getCategory() : "Venda Avulsa");
                name = name.replace("Venda: ", "");
                itemMap.put(name, itemMap.getOrDefault(name, 0.0) + r.getAmount().doubleValue());
                countMap.put(name, countMap.getOrDefault(name, 0) + 1);
            }
        }

        return itemMap.entrySet().stream()
                .map(e -> DashboardResponse.ServiceSummary.builder().serviceName(e.getKey()).revenue(e.getValue()).count(countMap.get(e.getKey())).build())
                .sorted((a, b) -> Double.compare(b.getRevenue(), a.getRevenue())).limit(5).collect(Collectors.toList());
    }

    private FinancialResponse mapRecordToResponse(FinancialRecord r) {
        return FinancialResponse.builder().id(r.getId()).type(r.getType()).category(r.getCategory()).amount(r.getAmount().doubleValue()).description(r.getDescription()).referenceDate(r.getReferenceDate()).paymentMethod(r.getPaymentMethod()).status(r.getStatus()).professionalName(r.getProfessional() != null ? r.getProfessional().getName() : "Empresa").clientName(r.getAppointment() != null ? r.getAppointment().getClient().getName() : "Cliente Balcão").build();
    }

    private FinancialResponse mapAppointmentToResponse(Appointment a) {
        return FinancialResponse.builder().id(a.getId()).type(FinancialType.INCOME).category("SERVICO").amount(a.getTotalPrice().doubleValue()).description("Agendamento: " + (a.getNotes() != null ? a.getNotes() : "Serviço realizado")).referenceDate(a.getEndTime()).paymentMethod("Indefinido").status("PAID").professionalName(a.getProfessional() != null ? a.getProfessional().getName() : "Profissional").clientName(a.getClient() != null ? a.getClient().getName() : "Cliente").build();
    }

    @Transactional
    public FinancialResponse createFinancialRecord(String companyId, FinancialRequest request) {
        Company company = companyRepository.findById(companyId).orElseThrow();
        Professional professional = request.getProfessionalId() != null ? professionalRepository.findByIdAndCompanyId(request.getProfessionalId(), companyId).orElse(null) : null;
        FinancialRecord record = FinancialRecord.builder().type(request.getType()).amount(BigDecimal.valueOf(request.getAmount())).description(request.getDescription()).title(request.getDescription()).company(company).professional(professional).referenceDate(request.getReferenceDate()).category(request.getCategory() != null ? request.getCategory() : "OUTROS").status("PAID").paymentMethod(request.getPaymentMethod()).build();
        return mapRecordToResponse(financialRecordRepository.save(record));
    }

    @Transactional
    public void deleteFinancialRecord(String companyId, String recordId) {
        FinancialRecord record = financialRecordRepository.findById(recordId).orElseThrow();
        if (!record.getCompany().getId().equals(companyId)) throw new BusinessException("Acesso negado");
        financialRecordRepository.delete(record);
    }

    public FinancialResponse getFinancialRecordById(String companyId, String recordId) {
        return mapRecordToResponse(financialRecordRepository.findById(recordId).orElseThrow());
    }

    public FinancialResponse updateFinancialRecord(String companyId, String recordId, FinancialRequest request) {
        FinancialRecord record = financialRecordRepository.findById(recordId).orElseThrow();
        record.setAmount(BigDecimal.valueOf(request.getAmount()));
        record.setDescription(request.getDescription());
        return mapRecordToResponse(financialRecordRepository.save(record));
    }

    public List<DashboardResponse.DailySummary> getRevenueChartData(String companyId) {
        return getDashboard(companyId, new DashboardRequest()).getDailyEvolution();
    }
}