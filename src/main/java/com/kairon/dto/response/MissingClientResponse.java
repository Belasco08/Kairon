package com.kairon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissingClientResponse {
    private String id;
    private String name;
    private String phone;
    private long daysAway;
    private String lastService;
    private LocalDateTime lastVisitDate;
}