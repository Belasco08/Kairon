package com.kairon.service;

import com.kairon.component.PlanGuard;
import com.kairon.domain.entity.*;
import com.kairon.domain.enums.AppointmentStatus;
import com.kairon.domain.enums.FinancialType;
import com.kairon.domain.enums.Role;
import com.kairon.dto.request.DashboardRequest;
import com.kairon.dto.request.FinancialRequest;
import com.kairon.dto.response.DashboardResponse;
import com.kairon.dto.response.FinancialResponse;
import com.kairon.dto.response.MonthlyHistoryResponse; // 👈 NOVO DTO
import com.kairon.exception.BusinessException;
import com.kairon.repository.*;
import com.kairon.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
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
        // Agora busca APENAS os registros oficiais do financeiro.
        // Como todo agendamento concluído já gera um FinancialRecord automaticamente,
        // não precisamos mais buscar e juntar a lista de Appointments.
        List<FinancialRecord> records = financialRecordRepository.findByCompanyIdOrderByReferenceDateDesc(companyId);

        return records.stream()
                .map(this::mapRecordToResponse)
                .collect(Collectors.toList());
    }


    /* =========================
       NOVO: HISTÓRICO SEMANAL (Para a Tabela do App)
       ========================= */
    @Transactional(readOnly = true)
    public List<MonthlyHistoryResponse> getWeeklyHistory(String companyId) {
        planGuard.checkPlusAccess(companyId);

        // Pega as últimas 6 semanas
        LocalDateTime startDate = LocalDate.now().minusWeeks(5).with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime endDate = LocalDate.now().atTime(LocalTime.MAX);

        // Busca apenas da fonte de verdade (FinancialRecord) para não duplicar!
        List<FinancialRecord> records = financialRecordRepository.findByCompanyIdAndReferenceDateBetween(companyId, startDate, endDate);

        Map<String, MonthlyHistoryResponse> weeklyMap = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        for (int i = 0; i < 6; i++) {
            LocalDate startOfWeek = LocalDate.now().minusWeeks(i).with(DayOfWeek.MONDAY);
            LocalDate endOfWeek = startOfWeek.plusDays(6);

            String key = startOfWeek.toString();
            String periodName = startOfWeek.format(formatter) + " até " + endOfWeek.format(formatter);

            // Reaproveitamos o MonthlyHistoryResponse, pois os campos são os mesmos!
            weeklyMap.put(key, MonthlyHistoryResponse.builder()
                    .id(key)
                    .period(periodName)
                    .income(0.0)
                    .expense(0.0)
                    .profit(0.0)
                    .margin("0%")
                    .build());
        }

        for (FinancialRecord r : records) {
            LocalDate startOfWeek = r.getReferenceDate().toLocalDate().with(DayOfWeek.MONDAY);
            String key = startOfWeek.toString();
            MonthlyHistoryResponse week = weeklyMap.get(key);

            if (week != null) {
                if (r.getType() == FinancialType.INCOME) {
                    week.setIncome(week.getIncome() + r.getAmount().doubleValue());
                } else if (r.getType() == FinancialType.EXPENSE) {
                    week.setExpense(week.getExpense() + r.getAmount().doubleValue());
                }
            }
        }

        for (MonthlyHistoryResponse week : weeklyMap.values()) {
            double profit = week.getIncome() - week.getExpense();
            week.setProfit(profit);
            if (week.getIncome() > 0) {
                double marginCalc = (profit / week.getIncome()) * 100;
                week.setMargin(String.format("%.0f%%", marginCalc));
            }
        }

        return new ArrayList<>(weeklyMap.values());
    }

    /* =========================
       DASHBOARD PRINCIPAL (GAMIFICAÇÃO + CFO DIGITAL)
       ========================= */

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String companyId, DashboardRequest request) {

        if (!planGuard.isPlus(companyId)) {
            String requestedPeriod = request.getPeriod() != null ? request.getPeriod().toLowerCase() : "month";
            if (!requestedPeriod.equals("day") && !requestedPeriod.equals("month")) {
                request.setPeriod("month");
            }
            request.setStartDate(null);
            request.setEndDate(null);
        }

        LocalDateTime startDate;
        LocalDateTime endDate = request.getEndDate() != null
                ? request.getEndDate().with(LocalTime.MAX)
                : LocalDate.now().atTime(LocalTime.MAX);

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

        List<FinancialRecord> financialRecords = financialRecordRepository.findByCompanyIdAndReferenceDateBetween(companyId, startDate, endDate);
        List<Appointment> appointments = appointmentRepository.findByCompanyIdAndStartTimeBetweenAndStatus(companyId, startDate, endDate, AppointmentStatus.COMPLETED);

        String currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow();

        // ==========================================================
        // 👇 VARIÁVEIS DA GAMIFICAÇÃO (VIDEOGAME DA EQUIPE) 👇
        // ==========================================================
        double todayRevenueCalc = 0.0;
        int todayAppointmentsCount = 0;
        double dailyGoalCalc = 200.0; // 🎯 META FIXA DO MVP
        String motivationMsg = "Bom dia! Vamos fazer dinheiro hoje!";

        if (currentUser.getRole() == Role.PROFESSIONAL) {
            Professional prof = professionalRepository.findByUserId(currentUserId).orElseThrow();

            financialRecords = financialRecords.stream().filter(r -> r.getProfessional() != null && r.getProfessional().getId().equals(prof.getId())).collect(Collectors.toList());
            appointments = appointments.stream().filter(a -> a.getProfessional() != null && a.getProfessional().getId().equals(prof.getId())).collect(Collectors.toList());

            LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
            LocalDateTime endOfToday = LocalDate.now().atTime(LocalTime.MAX);

            BigDecimal todayRev = appointmentRepository.sumProfessionalRevenueByDateRangeAndStatus(
                    companyId, prof.getId(), startOfToday, endOfToday, AppointmentStatus.COMPLETED);
            todayRevenueCalc = todayRev != null ? todayRev.doubleValue() : 0.0;

            todayAppointmentsCount = (int) appointments.stream()
                    .filter(a -> a.getStartTime().toLocalDate().isEqual(LocalDate.now()))
                    .count();

            if (todayRevenueCalc >= dailyGoalCalc) {
                motivationMsg = "🔥 Meta batida! Você é uma máquina de fazer dinheiro!";
            } else if (todayRevenueCalc >= dailyGoalCalc * 0.5) {
                motivationMsg = "🚀 Falta pouco pra bater a meta de hoje, acelera!";
            } else if (todayRevenueCalc > 0) {
                motivationMsg = "✂️ O dia começou bem, bora fechar essa meta!";
            } else {
                motivationMsg = "✂️ Bora dar o primeiro tapa no visual do dia!";
            }
        }

        // ==========================================================
        // 👇 PREVISIBILIDADE DE CAIXA (CFO DIGITAL) 👇
        // ==========================================================

        // 1. Separa as despesas em PAGAS e PENDENTES
        List<FinancialRecord> allExpenses = financialRecords.stream()
                .filter(r -> r.getType() == FinancialType.EXPENSE)
                .collect(Collectors.toList());

        BigDecimal paidExpenses = allExpenses.stream()
                .filter(r -> !"PENDING".equals(r.getStatus()))
                .map(FinancialRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<FinancialRecord> pendingExpenseRecords = allExpenses.stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .collect(Collectors.toList());

        BigDecimal pendingExpensesAmount = pendingExpenseRecords.stream()
                .map(FinancialRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Monta a lista das próximas 5 contas a vencer
        List<DashboardResponse.PendingPayable> upcomingPayablesList = pendingExpenseRecords.stream()
                .sorted(java.util.Comparator.comparing(r -> r.getReferenceDate() != null ? r.getReferenceDate() : LocalDateTime.MAX))
                .limit(5)
                .map(r -> DashboardResponse.PendingPayable.builder()
                        .id(r.getId())
                        .title(r.getTitle() != null ? r.getTitle() : "Conta a Pagar")
                        .amount(r.getAmount().doubleValue())
                        .dueDate(r.getReferenceDate() != null ? r.getReferenceDate().toString() : "")
                        .isOverdue(r.getReferenceDate() != null && r.getReferenceDate().isBefore(LocalDate.now().atStartOfDay()))
                        .build())
                .collect(Collectors.toList());

        // ==========================================================
        // 👇 BALANÇO GERAL 👇
        // ==========================================================

        BigDecimal recordsRevenue = financialRecords.stream()
                .filter(r -> r.getType() == FinancialType.INCOME)
                .filter(r -> !"PENDING".equals(r.getStatus())) // Não soma dinheiro que ainda não entrou
                .map(FinancialRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal servicesRevenue = appointments.stream().map(Appointment::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRevenue = recordsRevenue.add(servicesRevenue);

        // Saldo Real = Tudo que Entrou - Despesas que JÁ FORAM PAGAS
        BigDecimal balance = totalRevenue.subtract(paidExpenses);

        // Lucro Seguro = Saldo Real - As contas que vão vencer
        BigDecimal safeBalance = balance.subtract(pendingExpensesAmount);

        long totalCount = appointments.size() + financialRecords.stream().filter(r -> r.getType() == FinancialType.INCOME).count();
        BigDecimal averageTicket = totalCount > 0 ? totalRevenue.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        List<DashboardResponse.DailySummary> dailyEvolution = calculateCombinedDailyEvolution(financialRecords, appointments, startDate, endDate);
        List<DashboardResponse.ServiceSummary> topServices = calculateCombinedTopServices(financialRecords, appointments);

        return DashboardResponse.builder()
                .period(period)
                .revenue(totalRevenue.doubleValue())
                .expenses(paidExpenses.doubleValue())
                .balance(balance.doubleValue())
                .appointmentCount((int) appointments.size())
                .averageTicket(averageTicket.doubleValue())
                .dailyEvolution(dailyEvolution)
                .topServices(topServices)
                .busyHours(new java.util.ArrayList<>())

                // INJETANDO VIDEOGAME
                .todayRevenue(todayRevenueCalc)
                .todayAppointments(todayAppointmentsCount)
                .dailyGoal(dailyGoalCalc)
                .motivationMessage(motivationMsg)

                // INJETANDO CFO
                .pendingExpenses(pendingExpensesAmount.doubleValue())
                .safeBalance(safeBalance.doubleValue())
                .upcomingPayables(upcomingPayablesList)

                .build();
    }


    /* =========================
       NOVO: HISTÓRICO MENSAL DETALHADO (Para a Tabela do App)
       ========================= */
    @Transactional(readOnly = true)
    public List<MonthlyHistoryResponse> getMonthlyHistory(String companyId) {
        // Trava: Apenas assinantes PLUS têm acesso ao histórico retroativo
        planGuard.checkPlusAccess(companyId);

        // Pega os últimos 6 meses
        LocalDateTime startDate = LocalDate.now().minusMonths(5).with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime endDate = LocalDate.now().atTime(LocalTime.MAX);

        List<FinancialRecord> records = financialRecordRepository.findByCompanyIdAndReferenceDateBetween(companyId, startDate, endDate);
        List<Appointment> appointments = appointmentRepository.findByCompanyIdAndStartTimeBetweenAndStatus(companyId, startDate, endDate, AppointmentStatus.COMPLETED);

        Map<String, MonthlyHistoryResponse> monthlyMap = new LinkedHashMap<>();

        // Gera os 6 meses vazios para garantir que apareçam na tabela mesmo sem vendas
        for (int i = 0; i < 6; i++) {
            LocalDate monthDate = LocalDate.now().minusMonths(i);
            String key = monthDate.getYear() + "-" + String.format("%02d", monthDate.getMonthValue());
            String periodName = monthDate.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR")) + " " + monthDate.getYear();
            // Capitaliza a primeira letra (ex: "fevereiro" -> "Fevereiro")
            periodName = periodName.substring(0, 1).toUpperCase() + periodName.substring(1);

            monthlyMap.put(key, MonthlyHistoryResponse.builder()
                    .id(key)
                    .period(periodName)
                    .income(0.0)
                    .expense(0.0)
                    .profit(0.0)
                    .margin("0%")
                    .build());
        }

        // Soma Entradas e Saídas dos Registros Financeiros (Vendas Avulsas e Despesas)
        for (FinancialRecord r : records) {
            String key = r.getReferenceDate().getYear() + "-" + String.format("%02d", r.getReferenceDate().getMonthValue());
            MonthlyHistoryResponse month = monthlyMap.get(key);
            if (month != null) {
                if (r.getType() == FinancialType.INCOME) {
                    month.setIncome(month.getIncome() + r.getAmount().doubleValue());
                } else if (r.getType() == FinancialType.EXPENSE) {
                    month.setExpense(month.getExpense() + r.getAmount().doubleValue());
                }
            }
        }

        // Soma Entradas dos Serviços (Agendamentos Concluídos)
        for (Appointment a : appointments) {
            String key = a.getStartTime().getYear() + "-" + String.format("%02d", a.getStartTime().getMonthValue());
            MonthlyHistoryResponse month = monthlyMap.get(key);
            if (month != null) {
                month.setIncome(month.getIncome() + a.getTotalPrice().doubleValue());
            }
        }

        // Calcula Lucro e Margem
        for (MonthlyHistoryResponse month : monthlyMap.values()) {
            double profit = month.getIncome() - month.getExpense();
            month.setProfit(profit);

            if (month.getIncome() > 0) {
                double marginCalc = (profit / month.getIncome()) * 100;
                month.setMargin(String.format("%.0f%%", marginCalc));
            }
        }

        return new ArrayList<>(monthlyMap.values());
    }


    /* =========================
       GRÁFICO DE DESPESAS (PLUS ONLY)
       ========================= */

    @Transactional(readOnly = true)
    public List<com.kairon.dto.response.CategorySumResponse> getExpensesChartData(String companyId, DashboardRequest request) {

        planGuard.checkPlusAccess(companyId);

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


    /* =========================================================
       👇 NOVOS MÉTODOS: ACERTO DE CONTAS DA EQUIPE (FECHAMENTO)
       ========================================================= */

    @Transactional(readOnly = true)
    public com.kairon.dto.response.SettlementResponse getProfessionalSettlement(String companyId, String professionalId) {
        // Busca o profissional
        Professional prof = professionalRepository.findByIdAndCompanyId(professionalId, companyId)
                .orElseThrow(() -> new BusinessException("Profissional não encontrado"));

        // Busca TUDO que está PENDENTE para este profissional (Comissões e Vales)
        List<FinancialRecord> pendingRecords = financialRecordRepository.findByCompanyIdOrderByReferenceDateDesc(companyId)
                .stream()
                .filter(r -> r.getProfessional() != null && r.getProfessional().getId().equals(professionalId))
                .filter(r -> "PENDING".equals(r.getStatus()))
                .collect(Collectors.toList());

        double totalCommission = 0.0;
        double totalAdvances = 0.0; // Vales ou consumo interno

        for (FinancialRecord record : pendingRecords) {
            if ("COMISSAO".equals(record.getCategory())) {
                totalCommission += record.getAmount().doubleValue();
            } else if ("VALE".equals(record.getCategory()) || "CONSUMO".equals(record.getCategory())) {
                // Se ele pegou um vale da gaveta, é descontado do acerto
                totalAdvances += record.getAmount().doubleValue();
            }
        }

        double netPayout = totalCommission - totalAdvances;

        return com.kairon.dto.response.SettlementResponse.builder()
                .professionalId(prof.getId())
                .professionalName(prof.getName())
                .totalCommission(totalCommission)
                .totalAdvances(totalAdvances)
                .netPayout(netPayout)
                .pendingRecords(pendingRecords.stream().map(this::mapRecordToResponse).collect(Collectors.toList()))
                .build();
    }


    @Transactional
    public void addProfessionalAdvance(String companyId, String professionalId, Double amount, String description) {
        Professional prof = professionalRepository.findByIdAndCompanyId(professionalId, companyId)
                .orElseThrow(() -> new BusinessException("Profissional não encontrado"));

        FinancialRecord advance = FinancialRecord.builder()
                .type(FinancialType.EXPENSE) // É uma despesa para a empresa (adiantamento)
                .amount(BigDecimal.valueOf(amount))
                .title("Vale / Consumo")
                .description(description != null && !description.isEmpty() ? description : "Adiantamento ou Consumo interno")
                .category("VALE")
                .status("PENDING") // Fica pendente para ser abatido no Acerto de Contas!
                .company(prof.getCompany())
                .professional(prof)
                .referenceDate(LocalDateTime.now())
                .paymentMethod("INTERNO")
                .build();

        financialRecordRepository.save(advance);
    }

    @Transactional
    public void payProfessionalSettlement(String companyId, String professionalId) {
        // Trava de segurança da assinatura
        planGuard.checkPlusAccess(companyId);

        // Busca os mesmos registros pendentes
        List<FinancialRecord> pendingRecords = financialRecordRepository.findByCompanyIdOrderByReferenceDateDesc(companyId)
                .stream()
                .filter(r -> r.getProfessional() != null && r.getProfessional().getId().equals(professionalId))
                .filter(r -> "PENDING".equals(r.getStatus()))
                .collect(Collectors.toList());

        if (pendingRecords.isEmpty()) {
            throw new BusinessException("Não há valores pendentes para acerto deste profissional.");
        }

        // Muda todos para PAGO
        for (FinancialRecord record : pendingRecords) {
            record.setStatus("PAID");
            // Se quiser, pode setar a data de pagamento real aqui:
            // record.setUpdatedAt(LocalDateTime.now());
        }

        financialRecordRepository.saveAll(pendingRecords);
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