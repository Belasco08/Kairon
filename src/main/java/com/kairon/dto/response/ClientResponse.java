package com.kairon.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponse {

    private String id;
    private String name;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private String notes;
    private String companyId;
    private String externalId;

    // Campos calculados
    private Integer totalAppointments;
    private Double totalSpent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAppointment;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}