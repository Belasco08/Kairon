package com.kairon.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfessionalUpdateRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    private String description;

    private String email;

    private String Password;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phone;

    private String photoUrl;

    private JsonNode workHours;

    private JsonNode daysOff;

    private Boolean isActive;

    private Double commissionPercentage; // Novo campo (Ex: 50.0 para 50%)
}