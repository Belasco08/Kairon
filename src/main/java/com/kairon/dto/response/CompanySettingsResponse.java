package com.kairon.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class CompanySettingsResponse {

    private String companyId; // ID da empresa
    private String name;
    private String slug;
    private String businessType;
    private String timezone;

    // Mudado para Map genérico e renomeado
    private Map<String, Object> businessHours;

    private Integer slotDuration;
    private Integer bufferTime;
    private String currency;
    private String logoUrl;
    private String website;
    private Boolean isActive;
    private Boolean isVerified;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}