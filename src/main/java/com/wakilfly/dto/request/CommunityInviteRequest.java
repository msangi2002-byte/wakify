package com.wakilfly.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CommunityInviteRequest {

    @NotEmpty(message = "At least one user to invite is required")
    private List<UUID> userIds;
}
