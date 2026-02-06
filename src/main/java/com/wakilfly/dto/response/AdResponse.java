package com.wakilfly.dto.response;

import com.wakilfly.model.AdStatus;
import com.wakilfly.model.AdType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdResponse {
    private UUID id;
    private String title;
    private String description;
    private String imageUrl;
    private String videoUrl;
    private String targetUrl;
    private AdType type;
    private AdStatus status;
    private BigDecimal dailyBudget;
    private BigDecimal totalBudget;
    private String targetRegions;
    private String targetCategories;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer impressions;
    private Integer clicks;
    private BigDecimal amountSpent;
    private Double ctr; // Click-through rate
    private BusinessSummary business;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class BusinessSummary {
        private UUID id;
        private String name;
        private String logo;
    }
}
