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
    /** TEXT, IMAGE, VIDEO, VOICE, DOCUMENT, etc. */
    private String type;
    private LocalDateTime createdAt;
    private Boolean isRead;

    // Sender info
    private UUID senderId;
    private String senderName;
    private String senderProfilePic;

    // Helper for UI
    private Boolean isMe;

    /** Reply target: message you're replying to */
    private ReplyToInfo replyTo;

    @Data
    @Builder
    public static class ReplyToInfo {
        private UUID id;
        private String content;
        private String senderName;
    }
}
