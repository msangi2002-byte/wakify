package com.wakilfly.dto.response;

import com.wakilfly.model.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private ActorSummary actor;
    private String message;
    private NotificationType type;
    private UUID entityId;
    private Boolean isRead;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class ActorSummary {
        private UUID id;
        private String name;
        private String profilePic;
    }
}
