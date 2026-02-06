package com.wakilfly.dto.response;

import com.wakilfly.model.SubscriptionPlan;
import com.wakilfly.model.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    private UUID id;
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private BigDecimal amount;

    // Dates
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long daysRemaining;

    // Settings
    private Boolean autoRenew;

    // Business info
    private UUID businessId;
    private String businessName;

    // Status info
    private Boolean isActive;
    private Boolean isExpired;
    private Boolean isInGracePeriod;

    private LocalDateTime createdAt;
}
