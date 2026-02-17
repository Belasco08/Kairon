package com.kairon.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String avatar; // A URL da foto
    private String role;

    private String plan; // 👈 NOVO CAMPO: "FREE" ou "PLUS"
}