package com.wakilfly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    // User statistics
    private Long totalUsers;
    private Long activeUsers;
    private Long newUsersToday;
    private Long newUsersThisWeek;
    private Long newUsersThisMonth;

    // Business statistics
    private Long totalBusinesses;
    private Long activeBusinesses;
    private Long pendingBusinesses;

    // Agent statistics
    private Long totalAgents;
    private Long activeAgents;
    private Long pendingAgents;

    // Order statistics
    private Long totalOrders;
    private Long pendingOrders;
    private Long completedOrders;
    private Long cancelledOrders;

    // Revenue statistics
    private BigDecimal totalRevenue;
    private BigDecimal revenueToday;
    private BigDecimal revenueThisWeek;
    private BigDecimal revenueThisMonth;

    // Content statistics
    private Long totalPosts;
    private Long totalProducts;
    private Long totalPromotions;

    // Moderation statistics
    private Long pendingReports;
    private Long pendingWithdrawals;

    // Subscription statistics
    private Long activeSubscriptions;
    private Long expiringSubscriptions;

    // Activity breakdown
    private Map<String, Long> usersByRole;
    private Map<String, Long> ordersByStatus;
    private Map<String, Long> reportsByType;
}
