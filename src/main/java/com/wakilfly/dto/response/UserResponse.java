package com.wakilfly.dto.response;

import com.wakilfly.model.Role;
import com.wakilfly.model.Visibility;
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
public class UserResponse {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String bio;
    private String profilePic;
    private String coverPic;
    private Role role;
    private Boolean isVerified;
    private Boolean isActive;
    private Boolean onboardingCompleted;
    private Integer followersCount;
    private Integer followingCount;
    private Integer postsCount;
    private Boolean isFollowing; // For profile views
    private Boolean isBusiness;
    private UUID businessId;
    /** True if the user has a pending request to become a business (awaiting payment). */
    private Boolean hasPendingBusinessRequest;
    /** When hasPendingBusinessRequest: order ID to poll GET /payments/status/{orderId}. */
    private String pendingBusinessPaymentOrderId;

    // Extended Details
    private String work;
    private String education;
    private String currentCity;
    private String region;   // Mkoa
    private String country; // Taifa
    private Double latitude;  // Map pin (from registration/profile)
    private Double longitude;
    private String interests; // Hobbies comma-separated
    private Integer age;    // Computed from dateOfBirth
    private String hometown;
    private String relationshipStatus;
    private String gender;
    private java.time.LocalDate dateOfBirth;
    private String website;

    /** Who can see your profile (for settings screen) */
    private Visibility profileVisibility;
    /** Who can see your following/followers list */
    private Visibility followingListVisibility;

    private LocalDateTime createdAt;

    /** Last activity timestamp (heartbeat). */
    private LocalDateTime lastSeen;

    /** Computed: lastSeen within last 5 minutes. */
    private Boolean isOnline;
}
