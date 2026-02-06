package com.wakilfly.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MessageResponse {
    private UUID id;
    private String content;
    private String mediaUrl;
    private LocalDateTime createdAt;
    private Boolean isRead;

    // Sender info
    private UUID senderId;
    private String senderName;
    private String senderProfilePic;

    // Helper for UI
    private Boolean isMe;
}
