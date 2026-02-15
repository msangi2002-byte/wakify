package com.wakilfly.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class AgentDashboardResponse {
    private BigDecimal currentBalance;
    private BigDecimal totalEarnings;
    private BigDecimal todayEarnings;
    private BigDecimal pendingWithdrawals;
    private Integer totalBusinessesActivated;
    private Integer totalReferrals;
    // Package information
    private UUID packageId;
    private String packageName;
    private Integer packageMaxBusinesses;
    private Integer packageRemainingBusinesses;
}
