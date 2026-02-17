package com.kairon.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignUserToProfessionalRequest {

    @NotBlank(message = "User ID is required")
    private String userId;
}