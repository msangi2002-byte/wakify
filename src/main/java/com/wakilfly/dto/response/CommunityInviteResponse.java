package com.wakilfly.dto.response;

import com.wakilfly.model.CommunityInviteStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CommunityInviteResponse {

    private UUID id;
    private UUID communityId;
    private String communityName;
    private UUID inviterId;
    private String inviterName;
    private UUID inviteeId;
    private String inviteeName;
    private CommunityInviteStatus status;
    private LocalDateTime createdAt;
}
