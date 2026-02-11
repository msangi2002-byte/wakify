package com.wakilfly.dto.response;

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
public class ConversationSummary {

    private UUID otherUserId;
    private String otherUserName;
    private String otherUserProfilePic;
    private String lastMessageContent;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
    private Boolean archived;
}
