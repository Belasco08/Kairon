package com.kairon.dto.request;

import lombok.Data;

@Data
public class SellProductRequest {
    private Integer quantity;
    private String clientName;
    private String clientPhone; // 👈 NOVO: Telefone do cliente para o histórico e marketing
    private String paymentMethod;
}