package com.wakilfly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StreamingConfigResponse {
    private StunTurnConfig iceServers;
    private String broadcastUrl; // RTMP
    private String signalUrl; // WebRTC

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StunTurnConfig {
        private String stunUrl;
        private String turnUrl;
        private String turnUsername;
        private String turnPassword;
    }
}
