package com.wakilfly.dto.response;

import com.wakilfly.entity.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlansResponse {

    private List<PlanInfo> plans;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanInfo {
        private SubscriptionPlan plan;
        private String name;
        private String description;
        private BigDecimal price;
        private Integer durationDays;
        private BigDecimal savingsPercentage;
        private List<String> features;
    }
}
