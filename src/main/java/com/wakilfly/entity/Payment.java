package com.wakilfly.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Column(name = "payment_phone")
    private String paymentPhone;

    @Column(name = "description")
    private String description;

    @Column(name = "provider_response", columnDefinition = "TEXT")
    private String providerResponse;

    @Column(name = "related_entity_type")
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
