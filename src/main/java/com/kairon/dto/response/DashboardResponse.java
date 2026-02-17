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

    // 👇 ADICIONE ESTA CLASSE ESTÁTICA TAMBÉM
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusyHour {
        private String hour;
        private Integer count;
    }
}