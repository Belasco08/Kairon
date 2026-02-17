package com.kairon.dto.request;

import com.kairon.domain.enums.ProductType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequest {
    private String name;
    private String barcode;
    private ProductType type;

    private BigDecimal costPrice;
    private BigDecimal salePrice;

    private Integer stockQuantity;
    private Integer minStockLevel;

    private String photoUrl;
    private String companyId; // O ID da empresa vem aqui ou no path
}