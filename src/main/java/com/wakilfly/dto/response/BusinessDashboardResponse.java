package com.wakilfly.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BusinessDashboardResponse {
    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
    private BigDecimal monthRevenue;
    private Integer totalOrders;
    private Integer pendingOrders;
    private Integer completedOrders;
    private Integer totalProducts;
    private Integer activeProducts;
    private Integer totalViews;
    private Double averageRating;
    private Integer totalReviews;
}
