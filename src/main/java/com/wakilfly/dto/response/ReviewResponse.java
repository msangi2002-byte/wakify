package com.wakilfly.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReviewResponse {
    private UUID id;
    private PostResponse.UserSummary user;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
