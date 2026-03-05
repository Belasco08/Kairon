package com.kairon.dto.request;

import com.kairon.domain.enums.FinancialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FinancialRequest {

    @NotNull(message = "Type is required")
    private FinancialType type;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotBlank(message = "Description is required")
    private String description;

    private String appointmentId;

    private String professionalId;

    private String paymentMethod;

    @NotNull(message = "Reference date is required")
    private LocalDateTime referenceDate;

    private String category;

    // Dentro do seu FinancialRequest.java adicione:
    private String status;
    private String title;
}