package com.kairon.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ClientRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[0-9\\s\\-\\(\\)]{10,}$", message = "Phone number is invalid")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    private LocalDate birthDate;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}