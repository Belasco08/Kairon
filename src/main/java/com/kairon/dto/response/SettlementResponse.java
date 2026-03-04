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
public class SettlementResponse {
    private String professionalId;
    private String professionalName;
    private double totalCommission; // Quanto ele ganhou
    private double totalAdvances;   // Vales / Consumo (o que ele deve pra barbearia)
    private double netPayout;       // Valor final a receber (Comissão - Vales)
    private List<FinancialResponse> pendingRecords; // A lista do que está sendo pago
}