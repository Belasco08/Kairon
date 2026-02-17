package com.kairon.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AppointmentUpdateRequest {

    @NotEmpty(message = "Service IDs are required")
    private List<String> serviceIds;
}