package com.wakilfly.dto.response;

import com.wakilfly.model.ReportReason;
import com.wakilfly.model.ReportStatus;
import com.wakilfly.model.ReportType;
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
public class ReportResponse {

    private UUID id;
    private ReportType type;
    private UUID targetId;
    private ReportReason reason;
    private String description;
    private ReportStatus status;
    private UserSummary reporter;
    private UserSummary resolvedBy;
    private String resolutionNotes;
    private String actionTaken;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private UUID id;
        private String name;
        private String profilePic;
    }
}
