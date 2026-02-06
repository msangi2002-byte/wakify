package com.wakilfly.controller;

import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.LiveStreamResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.LiveStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/live")
@RequiredArgsConstructor
public class LiveStreamController {

    private final LiveStreamService liveStreamService;
    private final CustomUserDetailsService userDetailsService;

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
        LocalDateTime scheduledAt = LocalDateTime.parse((String) request.get("scheduledAt"));

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
}
