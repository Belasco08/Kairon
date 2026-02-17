package com.kairon.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentServiceResponse {

    private String id;
    private String name;
    private Double price;
    private Integer duration;
}
