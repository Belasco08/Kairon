package com.kairon.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateAppointmentRequest {

    @NotBlank(message = "Professional ID is required")
    private String professionalId;

    @NotEmpty(message = "At least one service is required")
    private List<String> serviceIds;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "Client information is required")
    private ClientInfo clientInfo;

    @Data
    public static class ClientInfo {

        @NotBlank(message = "Client name is required")
        private String name;

        @NotBlank(message = "Client phone is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$",
                message = "Phone number must be valid")
        private String phone;

        @Email(message = "Email should be valid")
        private String email;
    }
}