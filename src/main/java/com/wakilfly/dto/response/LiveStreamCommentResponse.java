package com.wakilfly.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LiveStreamCommentResponse {
    private UUID id;
    private UUID authorId;
    private String authorName;
    private String authorProfilePic;
    private String content;
    private LocalDateTime createdAt;
}
