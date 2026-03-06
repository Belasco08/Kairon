package com.kairon.dto.request;

import com.kairon.domain.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppointmentStatusRequest {

    @NotNull(message = "Status is required")
    private AppointmentStatus status;

    private String reason;

    // 👇 ADICIONADO PARA O FIADO
    private Boolean isPaid = true;

    // ... seus campos atuais (status, reason, isPaid)

    // 👇 ADICIONE ESTES DOIS CAMPOS 👇
    private String paymentMethod;
    private java.math.BigDecimal machineFeePercentage;


}