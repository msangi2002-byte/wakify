package com.wakilfly.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CommunityEventResponse {

    private UUID id;
    private UUID communityId;
    private UUID creatorId;
    private String creatorName;
    private String title;
    private String description;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String coverImage;
    private LocalDateTime createdAt;
}
