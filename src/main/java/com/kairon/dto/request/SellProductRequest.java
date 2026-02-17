package com.kairon.dto.request;

import lombok.Data;

@Data
public class SellProductRequest {
    private Integer quantity;
    private String clientName; // Para saber pra quem vendeu
    private String paymentMethod; // "DINHEIRO", "PIX", "CARTAO"
}