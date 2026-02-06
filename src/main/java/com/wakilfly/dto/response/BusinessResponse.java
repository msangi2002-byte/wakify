package com.wakilfly.dto.response;

import com.wakilfly.model.BusinessStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessResponse {

    private UUID id;
    private String name;
    private String description;
    private String category;
    private String logo;
    private String coverImage;

    private BusinessStatus status;
    private Boolean isVerified;

    private String region;
    private String district;
    private String ward;
    private String street;

    private Double latitude;
    private Double longitude;

    // Owner info
    private PostResponse.UserSummary owner;

    // Agent info
    private UUID agentId;
    private String agentName;
    private String agentCode;

    // Subscription info
    private Boolean hasActiveSubscription;
    private LocalDateTime subscriptionExpiresAt;

    // Stats
    private Integer productsCount;
    private Integer ordersCount;
    private Double rating;
    private Integer reviewsCount;

    private LocalDateTime createdAt;
}
