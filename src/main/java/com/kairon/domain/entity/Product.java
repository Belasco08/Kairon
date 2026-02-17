package com.kairon.domain.entity;

import com.kairon.domain.enums.ProductType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(nullable = false)
    private String name;

    private String barcode; // Código de barras

    @Lob // 👈 Indica que é um arquivo grande (Large Object)
    @Column(columnDefinition = "LONGTEXT") // 👈 Garante que cabe a foto inteira (MySQL/H2)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    private ProductType type;

    // Preços
    private BigDecimal costPrice; // Quanto você pagou
    private BigDecimal salePrice; // Quanto você vende (pode ser null se for CONSUMPTION)

    // Estoque
    private Integer stockQuantity;
    private Integer minStockLevel; // Para avisar quando estiver acabando


    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    private boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}