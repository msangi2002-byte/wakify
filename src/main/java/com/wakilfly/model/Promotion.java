package com.wakilfly.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id")
    private Business business;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionType type;

    /** Objective: AWARENESS, TRAFFIC, ENGAGEMENT, MESSAGES, LEADS, CONVERSIONS. Algorithm optimizes for this. */
    @Enumerated(EnumType.STRING)
    @Column(name = "objective", length = 32)
    private PromotionObjective objective;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PromotionStatus status = PromotionStatus.PENDING;

    // Reference to the promoted item
    @Column(name = "target_id")
    private UUID targetId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Budget and spending
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal budget;

    @Column(name = "spent_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal spentAmount = BigDecimal.ZERO;

    @Column(name = "daily_budget", precision = 12, scale = 2)
    private BigDecimal dailyBudget;

    // Duration
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    // Targeting (simplified)
    @Column(name = "target_regions")
    private String targetRegions; // Comma-separated regions

    @Column(name = "target_age_min")
    private Integer targetAgeMin;

    @Column(name = "target_age_max")
    private Integer targetAgeMax;

    @Column(name = "target_gender")
    private String targetGender; // ALL, MALE, FEMALE

    @Column(name = "target_country", length = 100)
    private String targetCountry;

    @Column(name = "target_city", length = 100)
    private String targetCity;

    @Column(name = "target_radius_km")
    private Integer targetRadiusKm;

    @Column(name = "target_interests", columnDefinition = "TEXT")
    private String targetInterests; // Comma-separated: Football, Fashion, etc.

    @Column(name = "target_behaviors", columnDefinition = "TEXT")
    private String targetBehaviors; // Comma-separated: Online shoppers, etc.

    /** Audience type: AUTOMATIC (followers+similar), LOCAL (region/age/gender), CUSTOM (pixel retargeting). */
    @Column(name = "audience_type", length = 32)
    private String audienceType;

    /** Ad quality score 0–100. Higher = better delivery in auction. */
    @Column(name = "ad_quality_score")
    private Double adQualityScore;

    /** Conversions during learning phase (target ~50 for algorithm to learn). */
    @Column(name = "learning_phase_conversions")
    @Builder.Default
    private Integer learningPhaseConversions = 0;

    // Performance metrics
    @Column(name = "impressions")
    @Builder.Default
    private Long impressions = 0L;

    @Column(name = "clicks")
    @Builder.Default
    private Long clicks = 0L;

    @Column(name = "conversions")
    @Builder.Default
    private Long conversions = 0L;

    @Column(name = "reach")
    @Builder.Default
    private Long reach = 0L;

    // Payment
    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "is_paid")
    @Builder.Default
    private Boolean isPaid = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public BigDecimal getRemainingBudget() {
        return budget.subtract(spentAmount != null ? spentAmount : BigDecimal.ZERO);
    }

    public Double getCtr() {
        if (impressions == null || impressions == 0)
            return 0.0;
        return (clicks != null ? clicks.doubleValue() : 0.0) / impressions * 100;
    }

    public Double getCostPerClick() {
        if (clicks == null || clicks == 0)
            return 0.0;
        return (spentAmount != null ? spentAmount.doubleValue() : 0.0) / clicks;
    }

    public void incrementImpressions() {
        this.impressions = (this.impressions != null ? this.impressions : 0L) + 1;
    }

    public void incrementClicks() {
        this.clicks = (this.clicks != null ? this.clicks : 0L) + 1;
    }

    public void incrementConversions() {
        this.conversions = (this.conversions != null ? this.conversions : 0L) + 1;
    }
}
