package com.wakilfly.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wakilfly.model.CommunityType;
import com.wakilfly.model.Visibility;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CommunityResponse {
    private UUID id;
    private String name;
    private String description;
    private CommunityType type;
    private Visibility privacy;
    private String coverImage;
    private Integer membersCount;
    private LocalDateTime createdAt;

    // Creator Info
    private UUID creatorId;
    private String creatorName;

    // Status relative to current user (JsonProperty so JSON has isMember/isAdmin not member/admin)
    @JsonProperty("isMember")
    private boolean isMember;
    @JsonProperty("isAdmin")
    private boolean isAdmin; // Creator or Admin

    /** If false, only creator/admins can post; if true, members can post. */
    private boolean allowMemberPosts;
}
