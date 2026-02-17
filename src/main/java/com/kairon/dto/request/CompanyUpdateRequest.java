package com.kairon.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Map;

@Data
public class CompanyUpdateRequest {

    // --- Campos Principais ---
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String name;

    private String phone;
    private String email;
    private String description;

    // --- Endereço ---
    private String address;
    private String city;
    private String state;
    private String zipCode;

    // 👇 O CAMPO IMPORTANTE
    private String whatsappTemplate;

    // --- Configurações Web/Logo ---
    private String website;
    private String logoUrl;

    // --- Campos JSON (Map) ---
    private Map<String, Object> settings;
    private Map<String, Object> businessHours;

    // --- Campos Extras ---
    private String businessType;
    private String timezone;
    private Integer slotDuration;
    private Integer bufferTime;
    private String currency;
}