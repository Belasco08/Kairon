package com.kairon.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicCompanyResponse {

    private String id;
    private String name;
    private String businessType;
    private String timezone;
    private JsonNode workHours;
    private Integer slotDuration;
    private Integer bufferTime;
    private String logoUrl;
    private String website;
}