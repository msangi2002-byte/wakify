package com.wakilfly.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CommunityPollResponse {

    private UUID id;
    private UUID communityId;
    private UUID creatorId;
    private String creatorName;
    private String question;
    private LocalDateTime endsAt;
    private List<PollOptionSummary> options;
    private UUID userVoteOptionId; // which option current user voted for (null if not voted)
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class PollOptionSummary {
        private UUID id;
        private String text;
        private int votesCount;
    }
}
