package com.wakilfly.dto.response;

import com.wakilfly.model.Role;
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
    private Integer followersCount;
    private Integer followingCount;
    private Integer postsCount;
    private Boolean isFollowing; // For profile views
    private Boolean isBusiness;
    private UUID businessId;
    private LocalDateTime createdAt;
}
