package com.wakilfly.dto.response;

import com.wakilfly.model.LiveStreamStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class LiveStreamResponse {
    private UUID id;
    private HostSummary host;
    private String title;
    private String description;
    private String thumbnailUrl;
    private LiveStreamStatus status;
    private String roomId;
    private String streamUrl; // HLS URL for playback
    private String rtmpUrl; // RTMP URL for broadcasting
    private String webrtcUrl; // WebRTC URL
    private Integer viewerCount;
    private Integer peakViewers;
    private BigDecimal totalGiftsValue;
    private Integer likesCount;
    private Integer commentsCount;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer durationSeconds;
    private LocalDateTime createdAt;

    /** Accepted guests on this live (stream key + URL). Viewers see "yupo live" for each. */
    private List<GuestStreamInfo> guestStreams;

    @Data
    @Builder
    public static class HostSummary {
        private UUID id;
        private String name;
        private String profilePic;
        private Boolean isVerified;
    }

    @Data
    @Builder
    public static class GuestStreamInfo {
        private String streamKey;
        private String streamUrl;
        private UUID requesterId;
        private String requesterName;
    }
}
