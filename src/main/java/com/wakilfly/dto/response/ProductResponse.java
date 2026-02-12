package com.wakilfly.dto.response;

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
public class ProductResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private String category;
    private Integer stockQuantity;
    private Boolean inStock;
    private Boolean isActive;

    // Images
    private List<ImageResponse> images;
    private String thumbnail; // Cover/thumbnail image URL (first image)

    // Business info
    private BusinessSummary business;

    // Stats
    private Integer viewsCount;
    private Integer ordersCount;
    private Double rating;
    private Integer reviewsCount;

    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageResponse {
        private UUID id;
        private String url;
        private Boolean isPrimary;
        private Integer displayOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessSummary {
        private UUID id;
        private String name;
        private String logo;
        private String region;
        private Boolean isVerified;
    }
}
