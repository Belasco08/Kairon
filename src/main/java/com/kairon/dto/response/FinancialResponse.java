package com.kairon.dto.response;

import com.kairon.domain.enums.FinancialType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialResponse {

    private String id;
    private FinancialType type;
    private String category;
    private Double amount;
    private String description;
    private String appointmentId;
    private String appointmentClientName;
    private String professionalId;
    private String professionalName;
    private String paymentMethod;
    private LocalDateTime referenceDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String status;
    private String clientName;
}