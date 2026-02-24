package com.wakilfly.dto.response;

import com.wakilfly.model.OrderSource;
import com.wakilfly.model.OrderStatus;
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
public class OrderResponse {

    private UUID id;
    private String orderNumber;
    private OrderStatus status;
    private OrderSource source;
    private UUID inquiryId;

    // Buyer info
    private UserSummary buyer;

    // Business info
    private BusinessSummary business;

    // Items
    private List<OrderItemResponse> items;
    private Integer totalItems;

    // Financials
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal discount;
    private BigDecimal total;

    // Payment
    private Boolean isPaid;
    private LocalDateTime paidAt;
    private String paymentMethod;

    // Delivery
    private String deliveryName;
    private String deliveryPhone;
    private String deliveryAddress;
    private String deliveryRegion;
    private String deliveryDistrict;

    // Notes
    private String customerNotes;
    private String sellerNotes;

    // Tracking
    private String trackingNumber;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private UUID id;
        private String name;
        private String phone;
        private String profilePic;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessSummary {
        private UUID id;
        private String name;
        private String logo;
        private String phone;
        private String email;
        private String website;
        private String region;
        private String district;
        private Double latitude;
        private Double longitude;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private UUID id;
        private UUID productId;
        private String productName;
        private String productImage;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal total;
    }
}
