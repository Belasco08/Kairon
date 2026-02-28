package com.kairon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyHistoryResponse {
    private String id;        // Ex: "2026-02"
    private String period;    // Ex: "Fevereiro 2026"
    private Double income;    // Total Entradas
    private Double expense;   // Total Saídas
    private Double profit;    // Lucro Líquido
    private String margin;    // Ex: "72%"
}