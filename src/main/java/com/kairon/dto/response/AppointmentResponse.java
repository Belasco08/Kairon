package com.kairon.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {

    private String id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Double totalPrice;

    private String notes;

    private ProfessionalResponse professional;
    private ClientResponse client;

    private String clientName;
    private String clientPhone;
    private String clientId;

    private String professionalId;
    private String professionalName;

    private List<AppointmentServiceResponse> services;
    private List<String> serviceNames;

    // 👇 ADICIONADO: Campos para o Histórico do Cliente
    private String lastServiceName;
    private String lastServiceDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String paymentMethod;
}