package com.kairon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalWithServicesResponse {

    private String id;
    private String name;
    private String description;
    private String phone;
    private String photoUrl;
    private boolean isActive;
    private List<ServiceResponse> services;
    private LocalDateTime createdAt;
    private String email;
}