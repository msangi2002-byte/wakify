package com.wakilfly.dto.response;

import com.wakilfly.model.CallStatus;
import com.wakilfly.model.CallType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CallResponse {
    private UUID id;
    private UserSummary caller;
    private UserSummary receiver;
    private CallType type;
    private CallStatus status;
    private String roomId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer durationSeconds;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class UserSummary {
        private UUID id;
        private String name;
        private String profilePic;
    }
}
