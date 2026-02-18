package com.wakilfly.dto.response;

import com.wakilfly.model.CommunityRole;
import com.wakilfly.model.CommunityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailResponse {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String bio;
    private String profilePic;
    private String coverPic;
    private com.wakilfly.model.Role role;
    private String adminRole;
    private Boolean isVerified;
    private Boolean isActive;
    private Boolean onboardingCompleted;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeen;

    // Extended profile
    private String work;
    private String education;
    private String currentCity;
    private String region;  // Mkoa
    private String country; // Taifa
    private String hometown;
    private String gender;
    private String language;
    private String interests;

    // Engagement stats
    private long followersCount;
    private long followingCount;
    private long postsCount;

    // Communities / Groups / Channels
    private List<CommunityMembership> communities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommunityMembership {
        private UUID communityId;
        private String name;
        private CommunityType type; // GROUP or CHANNEL
        private CommunityRole memberRole; // ADMIN, MODERATOR, MEMBER
        private Integer membersCount;
        private LocalDateTime joinedAt;
    }
}
