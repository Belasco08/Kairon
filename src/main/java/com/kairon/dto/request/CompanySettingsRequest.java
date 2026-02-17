package com.kairon.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
public class CompanySettingsRequest {

    // Removemos validações @NotBlank de campos que podem não ser enviados pela tela de Settings
    private String name;
    private String businessType;

    // Mudado para Map genérico e renomeado para businessHours
    @NotNull(message = "Business hours are required")
    private Map<String, Object> businessHours;

    private Integer slotDuration;
    private Integer bufferTime;
    private String currency;

    private String logoUrl;
    private String website;
}