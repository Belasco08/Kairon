package com.kairon.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    private String name;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String password;

    @Pattern(
            regexp = "^$|^\\(?\\d{2}\\)?\\s?\\d{4,5}-?\\d{4}$",
            message = "Telefone inválido. Use o formato (11) 99999-9999"
    )
    private String phone;

    @NotBlank(message = "Nome da empresa é obrigatório")
    @Size(min = 3, max = 100, message = "Nome da empresa deve ter entre 3 e 100 caracteres")
    private String companyName;

    @NotBlank(message = "Tipo de negócio é obrigatório")
    @Size(min = 3, max = 50, message = "Tipo de negócio deve ter entre 3 e 50 caracteres")
    private String businessType;
}