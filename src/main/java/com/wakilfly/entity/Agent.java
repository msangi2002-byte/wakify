package com.wakilfly.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "agents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "license_number")
    private String licenseNumber;

    @Column(name = "id_number")
    private String idNumber;

    @Column(name = "id_document_url")
    private String idDocumentUrl;

    @Column(name = "agent_code", unique = true)
    private String agentCode; // Referral code

    @Column(name = "registration_fee_paid", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal registrationFeePaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AgentStatus status = AgentStatus.PENDING;

    // Businesses activated by this agent
    @OneToMany(mappedBy = "agent", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Business> businesses = new ArrayList<>();

    // Commissions
    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Commission> commissions = new ArrayList<>();

    @Column(name = "total_earnings", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Column(name = "available_balance", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public int getBusinessesCount() {
        return businesses != null ? businesses.size() : 0;
    }
}
