package com.wakilfly.dto.response;

import com.wakilfly.model.JoinRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class JoinRequestResponse {
    private UUID id;
    private UUID liveStreamId;
    private RequesterSummary requester;
    private JoinRequestStatus status;
    private LocalDateTime hostRespondedAt;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class RequesterSummary {
        private UUID id;
        private String name;
        private String profilePic;
        private Boolean isVerified;
    }
}
