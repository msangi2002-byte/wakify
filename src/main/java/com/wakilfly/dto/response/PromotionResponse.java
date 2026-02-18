package com.wakilfly.dto.response;

import com.wakilfly.model.PromotionObjective;
import com.wakilfly.model.PromotionStatus;
import com.wakilfly.model.PromotionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionResponse {

    private UUID id;
    private PromotionType type;
    private PromotionStatus status;
    private PromotionObjective objective;
    private UUID targetId;
    private String title;
    private String description;

    // Budget info
    private BigDecimal budget;
    private BigDecimal spentAmount;
    private BigDecimal remainingBudget;
    private BigDecimal dailyBudget;

    // Duration
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Targeting
    private List<String> targetRegions;
    private Integer targetAgeMin;
    private Integer targetAgeMax;
    private String targetGender;
    private String targetCountry;
    private String targetCity;
    private Integer targetRadiusKm;
    private List<String> targetInterests;
    private List<String> targetBehaviors;
    private Double adQualityScore;
    private Integer learningPhaseConversions;
    private Boolean isInLearningPhase;

    // Performance metrics
    private Long impressions;
    private Long clicks;
    private Long conversions;
    private Long reach;
    private Double ctr; // Click-through rate
    private Double costPerClick;

    // Payment
    private Boolean isPaid;
    private UUID userId;
    private String userName;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromotionStats {
        private Long totalImpressions;
        private Long totalClicks;
        private Long totalConversions;
        private BigDecimal totalSpent;
        private Double averageCtr;
    }
}
