package com.wakilfly.controller;

import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.JoinRequestResponse;
import com.wakilfly.dto.response.LiveStreamResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.StreamingConfigResponse;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.LiveStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/live")
@RequiredArgsConstructor
public class LiveStreamController {

    private final LiveStreamService liveStreamService;
    private final CustomUserDetailsService userDetailsService;

    @Value("${streaming.stun.url}")
    private String stunUrl;

    @Value("${streaming.turn.url}")
    private String turnUrl;

    @Value("${streaming.turn.username}")
    private String turnUsername;

    @Value("${streaming.turn.password}")
    private String turnPassword;

    @Value("${streaming.rtmp-url}")
    private String rtmpBaseUrl;

    @Value("${streaming.webrtc-signal-url}")
    private String webrtcSignalUrl;

    @Value("${streaming.srs-base-url:https://streaming.wakilfy.com}")
    private String srsBaseUrl;

    /**
     * Create/Start a live stream
     * POST /api/v1/live/start
     */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<LiveStreamResponse>> startLive(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {
        UUID hostId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        String title = request.get("title");
        String description = request.get("description");

        LiveStreamResponse stream = liveStreamService.createLiveStream(hostId, title, description, null);
        return ResponseEntity.ok(ApiResponse.success("Live stream started", stream));
    }

    /**
     * Schedule a live stream
     * POST /api/v1/live/schedule
     */
    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<LiveStreamResponse>> scheduleLive(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> request) {
        UUID hostId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        String title = (String) request.get("title");
        String description = (String) request.get("description");
        java.time.LocalDateTime scheduledAt = java.time.LocalDateTime.parse((String) request.get("scheduledAt"));

        LiveStreamResponse stream = liveStreamService.createLiveStream(hostId, title, description, scheduledAt);
        return ResponseEntity.ok(ApiResponse.success("Live stream scheduled", stream));
    }

    /**
     * End a live stream
     * POST /api/v1/live/{liveId}/end
     */
    @PostMapping("/{liveId}/end")
    public ResponseEntity<ApiResponse<LiveStreamResponse>> endLive(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID liveId) {
        UUID hostId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        LiveStreamResponse stream = liveStreamService.endLiveStream(liveId, hostId);
        return ResponseEntity.ok(ApiResponse.success("Live stream ended", stream));
    }

    /**
     * Get active live streams
     * GET /api/v1/live/active
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<LiveStreamResponse>>> getActiveLives(
            @RequestParam(defaultValue = "20") int limit) {
        List<LiveStreamResponse> streams = liveStreamService.getActiveLiveStreams(limit);
        return ResponseEntity.ok(ApiResponse.success(streams));
    }

    /**
     * Get live stream details
     * GET /api/v1/live/{liveId}
     */
    @GetMapping("/{liveId}")
    public ResponseEntity<ApiResponse<LiveStreamResponse>> getLiveStream(@PathVariable UUID liveId) {
        LiveStreamResponse stream = liveStreamService.getLiveStream(liveId);
        return ResponseEntity.ok(ApiResponse.success(stream));
    }

    /**
     * Join a live stream
     * POST /api/v1/live/{liveId}/join
     */
    @PostMapping("/{liveId}/join")
    public ResponseEntity<ApiResponse<LiveStreamResponse>> joinLive(@PathVariable UUID liveId) {
        LiveStreamResponse stream = liveStreamService.joinLiveStream(liveId);
        return ResponseEntity.ok(ApiResponse.success("Joined live stream", stream));
    }

    /**
     * Leave a live stream
     * POST /api/v1/live/{liveId}/leave
     */
    @PostMapping("/{liveId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveLive(@PathVariable UUID liveId) {
        liveStreamService.leaveLiveStream(liveId);
        return ResponseEntity.ok(ApiResponse.success("Left live stream"));
    }

    /**
     * Like a live stream
     * POST /api/v1/live/{liveId}/like
     */
    @PostMapping("/{liveId}/like")
    public ResponseEntity<ApiResponse<Void>> likeLive(@PathVariable UUID liveId) {
        liveStreamService.likeLiveStream(liveId);
        return ResponseEntity.ok(ApiResponse.success("Liked"));
    }

    /**
     * Get my live stream history
     * GET /api/v1/live/my-streams
     */
    @GetMapping("/my-streams")
    public ResponseEntity<ApiResponse<PagedResponse<LiveStreamResponse>>> getMyStreams(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID hostId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<LiveStreamResponse> streams = liveStreamService.getUserLiveStreams(hostId, page, size);
        return ResponseEntity.ok(ApiResponse.success(streams));
    }

    // ---------- Join request (guest) ----------

    /**
     * Request to join live as guest
     * POST /api/v1/live/{liveId}/join-request
     */
    @PostMapping("/{liveId}/join-request")
    public ResponseEntity<ApiResponse<JoinRequestResponse>> requestToJoin(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("liveId") UUID liveId) {
        UUID requesterId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        JoinRequestResponse response = liveStreamService.requestToJoinLive(liveId, requesterId);
        return ResponseEntity.ok(ApiResponse.success("Join request sent", response));
    }

    /**
     * Get join requests for a live (host only). ?pendingOnly=true default
     * GET /api/v1/live/{liveId}/join-requests
     */
    @GetMapping("/{liveId}/join-requests")
    public ResponseEntity<ApiResponse<List<JoinRequestResponse>>> getJoinRequests(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("liveId") UUID liveId,
            @RequestParam(defaultValue = "true") boolean pendingOnly) {
        UUID hostId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        List<JoinRequestResponse> list = liveStreamService.getJoinRequests(liveId, hostId, pendingOnly);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * Accept a join request (host only)
     * POST /api/v1/live/join-requests/{requestId}/accept
     */
    @PostMapping("/join-requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<JoinRequestResponse>> acceptJoinRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID requestId) {
        UUID hostId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        JoinRequestResponse response = liveStreamService.acceptJoinRequest(requestId, hostId);
        return ResponseEntity.ok(ApiResponse.success("Join request accepted", response));
    }

    /**
     * Reject a join request (host only)
     * POST /api/v1/live/join-requests/{requestId}/reject
     */
    @PostMapping("/join-requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<JoinRequestResponse>> rejectJoinRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID requestId) {
        UUID hostId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        JoinRequestResponse response = liveStreamService.rejectJoinRequest(requestId, hostId);
        return ResponseEntity.ok(ApiResponse.success("Join request rejected", response));
    }

    /**
     * Get streaming configuration (STUN/TURN)
     * GET /api/v1/live/config
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<StreamingConfigResponse>> getStreamingConfig() {
        StreamingConfigResponse.StunTurnConfig iceServers = StreamingConfigResponse.StunTurnConfig.builder()
                .stunUrl(stunUrl)
                .turnUrl(turnUrl)
                .turnUsername(turnUsername)
                .turnPassword(turnPassword)
                .build();

        String rtcApiBaseUrl = (srsBaseUrl.endsWith("/") ? srsBaseUrl : srsBaseUrl + "/") + "rtc/v1";
        StreamingConfigResponse config = StreamingConfigResponse.builder()
                .iceServers(iceServers)
                .broadcastUrl(rtmpBaseUrl)
                .signalUrl(webrtcSignalUrl)
                .rtcApiBaseUrl(rtcApiBaseUrl)
                .build();

        return ResponseEntity.ok(ApiResponse.success(config));
    }
}
