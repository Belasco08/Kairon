package com.kairon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private String period;
    private Double revenue;
    private Double expenses;
    private Double balance;
    private Integer appointmentCount;
    private Double averageTicket;

    // 👇 CAMPOS DA GAMIFICAÇÃO DA EQUIPE 👇
    private Double todayRevenue;
    private Integer todayAppointments;
    private Double dailyGoal;
    private String motivationMessage;

    // ==========================================
    // 👇 NOVOS CAMPOS: PREVISIBILIDADE DE CAIXA (CFO) 👇
    // ==========================================
    private Double pendingExpenses; // Total de contas a pagar no período
    private Double safeBalance;     // Lucro Seguro (Balance - PendingExpenses)

    @Builder.Default
    private List<PendingPayable> upcomingPayables = List.of(); // Lista das próximas contas

    private List<BusyHour> busyHours;

    @Builder.Default
    private List<DailySummary> dailyEvolution = List.of();

    @Builder.Default
    private List<ServiceSummary> topServices = List.of();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySummary {
        private String date;
        private Double revenue;
        private Integer appointments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceSummary {
        private String serviceId;
        private String serviceName;
        private Double revenue;
        private Integer count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusyHour {
        private String hour;
        private Integer count;
    }

    // 👇 NOVA CLASSE PARA AS CONTAS A PAGAR 👇
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingPayable {
        private String id;
        private String title;
        private Double amount;
        private String dueDate; // Data de vencimento
        private Boolean isOverdue; // Se está atrasada
    }
}