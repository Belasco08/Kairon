package com.kairon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse {

    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer duration;
    private String color;
    private String category;
    private boolean isActive;
    private boolean onlineBooking;
    private String companyId;
    private String professionalId;
    private String professionalName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}