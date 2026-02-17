package com.kairon.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceUpdateRequest {

    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    @Min(value = 0, message = "Price must be greater than or equal to 0")
    private BigDecimal price;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer duration;

    @Size(max = 7, message = "Color code invalid")
    private String color;

    private Boolean isActive;

    private Boolean onlineBooking;

    @Size(max = 50, message = "Category must be at most 50 characters")
    private String category;

    private String professionalId;
}