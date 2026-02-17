package com.kairon.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DashboardRequest {
    // day, week, month, year
    @NotBlank(message = "Period is required")
    private String period;

    // Opcionais (caso queira implementar filtro personalizado de datas no futuro)
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private List<ChartData> salesChart;   // Para o Gráfico de Linhas (Vendas x Tempo)
    private List<ServiceData> topServices; // Para o Gráfico de Pizza (Serviços mais vendidos)

    @Data
    @Builder
    public static class ChartData {
        private String label;  // Ex: "08:00", "Seg", "01/02"
        private Double value;  // Valor vendido nesse ponto
    }

    @Data
    @Builder
    public static class ServiceData {
        private String name;   // Ex: "Corte de Cabelo"
        private Integer count; // Quantas vezes foi vendido
        private Double total;  // Valor total arrecadado com ele
    }
}