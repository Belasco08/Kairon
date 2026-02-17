package com.kairon.domain.entity;

import com.kairon.domain.enums.PlanType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    // --- NOVOS CAMPOS DE CONTATO E ENDEREÇO ---

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(columnDefinition = "TEXT") // Permite descrições longas
    private String description;

    private String address;
    private String city;
    private String state;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(columnDefinition = "TEXT")
    private String whatsappTemplate; // O texto personalizado

    // --- CAMPOS JSON (SETTINGS E HORÁRIOS) ---

    // Substitui o antigo 'workHours' para alinhar com o Frontend/Service
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "business_hours", columnDefinition = "json")
    private Map<String, Object> businessHours;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> settings;

    // --- CAMPOS LEGADOS/CONFIG ---

    @Column(name = "business_type", nullable = false, length = 50)
    private String businessType;

    @Column(nullable = false, length = 50)
    private String timezone = "America/Sao_Paulo";

    @Column(name = "slot_duration")
    private Integer slotDuration = 30;

    @Column(name = "buffer_time")
    private Integer bufferTime = 10;

    @Column(nullable = false, length = 3)
    private String currency = "BRL";

    @Column(name = "logo_url", length = 500)
    private String logoUrl; // O método setLogoUrl será gerado pelo Lombok aqui

    @Column(length = 100)
    private String website;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    /* =========================
       RELATIONSHIPS
       ========================= */

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<User> users = new HashSet<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Professional> professionals = new HashSet<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Services> services = new HashSet<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Appointment> appointments = new HashSet<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Client> clients = new HashSet<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<FinancialRecord> financialRecords = new HashSet<>();

    /* =========================
       AUDIT
       ========================= */

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /* =========================
       LIFECYCLE
       ========================= */

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Método auxiliar para compatibilidade se em algum lugar chamar setLogo() ao invés de setLogoUrl()
    public void setLogo(String logo) {
        this.logoUrl = logo;
    }

    public String getLogo() {
        return this.logoUrl;
    }


    // Adicione isso aos atributos da sua Company
    @Column(name = "last_milestone_achieved")
    private Integer lastMilestoneAchieved = 0; // Vai guardar o valor: 10000, 50000, etc.


    @Column(name = "plan_expires_at")
    private LocalDateTime planExpiresAt;

    // PLUS
    @Builder.Default // 👈 ADICIONE ISSO AQUI
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType plan = PlanType.FREE; // 👈 O segredo está aqui! (Já nasce FREE)

    // Opcional: Data de expiração se você for vender assinatura mensal
    private LocalDateTime subscriptionExpiresAt;
}