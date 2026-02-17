package com.kairon.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String code; // Ex: KRN-BRONZE-X7B9

    @Column(name = "free_months")
    private Integer freeMonths; // Quantos meses grátis esse cupom dá

    @Column(name = "discount_percentage")
    private Double discountPercentage; // Se for dar desconto (ex: 50%)

    @Column(name = "is_used")
    private boolean isUsed; // Para ninguém usar o mesmo cupom duas vezes

    @ManyToOne
    @JoinColumn(name = "used_by_company_id")
    private Company usedByCompany; // Quem usou o cupom

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Opcional: validade do cupom

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}