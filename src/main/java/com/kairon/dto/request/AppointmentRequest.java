package com.kairon.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AppointmentRequest {

    // REMOVIDO O @NotBlank para permitir null (caso seja o próprio profissional agendando)
    private String professionalId;

    @NotEmpty(message = "At least one service is required")
    private List<String> serviceIds;

    @NotNull(message = "Start time is required")
    // @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotBlank(message = "Client name is required")
    private String clientName;

    @NotBlank(message = "Client phone is required")
    private String clientPhone;

    private String clientEmail;

    private String notes;
}