package com.kairon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceListResponse {

    private List<ServiceResponse> services;
    private int total;
    private int page;
    private int size;
}