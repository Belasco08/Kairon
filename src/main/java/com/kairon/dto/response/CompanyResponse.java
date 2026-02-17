package com.kairon.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Opcional: Não envia campos nulos para o front
public class CompanyResponse {

    private String id;
    private String name;
    private String slug;
    private String businessType;
    private String timezone;

    // 👇 Mudança importante: De JsonNode para Map, e renomeado para businessHours
    private Map<String, Object> businessHours;

    // 👇 Novo campo para configurações gerais (JSON)
    private Map<String, Object> settings;

    private Integer slotDuration;
    private Integer bufferTime;
    private String currency;
    private String logoUrl;
    private String website;

    private String whatsappTemplate;

    private boolean isActive;
    private boolean isVerified;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 👇 Campos de Contato (Já existiam, mantidos)
    private String email;
    private String phone;

    // 👇 CAMPOS NOVOS (Adicionados para corrigir o erro do Builder)
    private String description;
    private String address;
    private String city;
    private String state;
    private String zipCode;
}