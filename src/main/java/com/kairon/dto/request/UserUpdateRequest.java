package com.kairon.dto.request;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String name;
    private String phone;
    private String currentPassword; // Opcional, para troca de senha
    private String newPassword;     // Opcional
}