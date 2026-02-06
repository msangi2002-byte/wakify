package com.wakilfly.dto.response;

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
public class CommentResponse {

    private UUID id;
    private String content;
    private PostResponse.UserSummary author;
    private UUID parentId;
    private Integer likesCount;
    private Integer repliesCount;
    private List<CommentResponse> replies;
    private LocalDateTime createdAt;
}
