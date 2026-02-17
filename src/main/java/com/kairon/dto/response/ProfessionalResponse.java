package com.kairon.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalResponse {

    private String id;
    private String name;
    private String description;
    private String photoUrl;
    private String phone;
    private boolean isActive;
    private JsonNode workHours;
    private JsonNode daysOff;
    private String companyId;
    private String userId;
    private List<ServiceSimpleResponse> services;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double commissionPercentage;


    private String email;



    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceSimpleResponse {
        private String id;
        private String name;
        private Double price;
        private Integer duration;
    }
}