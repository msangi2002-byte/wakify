package com.wakilfly.dto.response;

import com.wakilfly.model.PromotionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionPackageResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationDays;
    private Long dailyReach;
    private Long totalImpressions;
    private PromotionType promotionType;
    private Boolean includesTargeting;
    private Boolean includesAnalytics;
}
