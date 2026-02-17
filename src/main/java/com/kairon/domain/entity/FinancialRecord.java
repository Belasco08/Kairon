package com.kairon.domain.entity;

import com.kairon.domain.enums.FinancialType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class FinancialRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // 👇 CAMPO NOVO: Título curto (Ex: "Compra de Estoque")
    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FinancialType type; // INCOME ou EXPENSE

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = true, length = 200) // Pode ser null as vezes
    private String description;

    // 👇 CAMPO NOVO: Categoria (Ex: "Estoque", "Aluguel", "Serviço")
    @Column(length = 50)
    private String category;

    // 👇 CAMPO NOVO: Status (Ex: "PAID", "PENDING")
    @Column(length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id")
    private Professional professional;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "reference_date", nullable = false)
    private LocalDateTime referenceDate;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}