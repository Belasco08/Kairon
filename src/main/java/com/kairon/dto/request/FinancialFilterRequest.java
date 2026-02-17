package com.kairon.dto.request;

import com.kairon.domain.enums.FinancialType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FinancialFilterRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    private List<FinancialType> types;

    private String professionalId;

    private Integer page = 0;

    private Integer size = 20;
}