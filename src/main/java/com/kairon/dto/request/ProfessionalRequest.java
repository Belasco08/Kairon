package com.kairon.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfessionalRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    // --- NOVOS CAMPOS PARA LOGIN ---
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String password; // Opcional (se não mandar, o service gera uma padrão)
    // -------------------------------

    private String description;

    @NotBlank(message = "Phone is required")
    // Regex ajustada para ser mais permissiva ou manter a sua se preferir
    @Pattern(regexp = "^\\+?[0-9(). -]{8,20}$", message = "Invalid phone number format")
    private String phone;

    private String photoUrl;

    private JsonNode workHours;

    private JsonNode daysOff;

    private Double commissionPercentage;

    private boolean isOwner;
    private String role; // "OWNER" ou "PROFESSIONAL"
}
