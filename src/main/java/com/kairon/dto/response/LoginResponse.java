package com.kairon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String userId;
    private String name;
    private String email;
    private String role;
    private String companyId;
    private String plan; // 👈 O campo novo que vai dizer "FREE" ou "PLUS"
}