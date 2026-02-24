package com.wakilfly.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BusinessFeedbackResponse {
    private UUID id;
    private PostResponse.UserSummary user;
    private String content;
    private LocalDateTime createdAt;
    private Boolean read;
    private LocalDateTime readAt;
}
