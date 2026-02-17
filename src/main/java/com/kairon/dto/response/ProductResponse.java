package com.kairon.dto.response;

import com.kairon.domain.enums.ProductType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ProductResponse {
    private String id;
    private String name;
    private String barcode;
    private ProductType type;
    private BigDecimal costPrice;
    private BigDecimal salePrice;
    private Integer stockQuantity;
    private Integer minStockLevel;
    private String photoUrl;
}