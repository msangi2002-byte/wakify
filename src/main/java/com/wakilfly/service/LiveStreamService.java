package com.wakilfly.service;

import com.wakilfly.dto.response.LiveStreamResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.*;
import com.wakilfly.repository.LiveStreamRepository;
import com.wakilfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveStreamService {

    private final LiveStreamRepository liveStreamRepository;
    private final UserRepository userRepository;

    @Value("${streaming.rtmp-url}")
    private String rtmpBaseUrl;

    @Value("${streaming.hls-url}")
    private String hlsBaseUrl;

    @Value("${streaming.webrtc-signal-url}")
    private String webrtcSignalUrl;

    /**
     * Create/Schedule a live stream
     */
    @Transactional
    public LiveStreamResponse createLiveStream(UUID hostId, String title, String description,
            LocalDateTime scheduledAt) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if user already has active live stream
        if (liveStreamRepository.findActiveByHostId(hostId).isPresent()) {
            throw new BadRequestException("You already have an active live stream");
        }

        String roomId = "live_" + UUID.randomUUID().toString().substring(0, 12);
        String streamKey = UUID.randomUUID().toString().replace("-", "");

        LiveStream liveStream = LiveStream.builder()
                .host(host)
                .title(title)
                .description(description)
                .roomId(roomId)
                .streamKey(streamKey)
                .status(scheduledAt != null ? LiveStreamStatus.SCHEDULED : LiveStreamStatus.LIVE)
                .scheduledAt(scheduledAt)
                .startedAt(scheduledAt == null ? LocalDateTime.now() : null)
                .build();

        liveStream = liveStreamRepository.save(liveStream);
        log.info("Live stream created: {} by host {}", roomId, host.getName());

        return mapToLiveStreamResponse(liveStream);
    }

    /**
     * Start a scheduled live stream
     */
    @Transactional
    public LiveStreamResponse startLiveStream(UUID liveStreamId, UUID hostId) {
        LiveStream liveStream = liveStreamRepository.findById(liveStreamId)
                .orElseThrow(() -> new ResourceNotFoundException("Live stream not found"));

        if (!liveStream.getHost().getId().equals(hostId)) {
            throw new BadRequestException("You are not the host of this stream");
        }

        if (liveStream.getStatus() != LiveStreamStatus.SCHEDULED) {
            throw new BadRequestException("Live stream cannot be started");
        }

        liveStream.setStatus(LiveStreamStatus.LIVE);
        liveStream.setStartedAt(LocalDateTime.now());
        liveStream = liveStreamRepository.save(liveStream);

        log.info("Live stream started: {}", liveStreamId);
        return mapToLiveStreamResponse(liveStream);
    }

    /**
     * End a live stream
     */
    @Transactional
    public LiveStreamResponse endLiveStream(UUID liveStreamId, UUID hostId) {
        LiveStream liveStream = liveStreamRepository.findById(liveStreamId)
                .orElseThrow(() -> new ResourceNotFoundException("Live stream not found"));

        if (!liveStream.getHost().getId().equals(hostId)) {
            throw new BadRequestException("You are not the host of this stream");
        }

        liveStream.setStatus(LiveStreamStatus.ENDED);
        liveStream.setEndedAt(LocalDateTime.now());

        if (liveStream.getStartedAt() != null) {
            long seconds = Duration.between(liveStream.getStartedAt(), liveStream.getEndedAt()).getSeconds();
            liveStream.setDurationSeconds((int) seconds);
        }

        liveStream = liveStreamRepository.save(liveStream);
        log.info("Live stream ended: {}, duration: {}s, total gifts: {}",
                liveStreamId, liveStream.getDurationSeconds(), liveStream.getTotalGiftsValue());

        return mapToLiveStreamResponse(liveStream);
    }

    /**
     * Join a live stream (increment viewer count)
     */
    @Transactional
    public LiveStreamResponse joinLiveStream(UUID liveStreamId) {
        LiveStream liveStream = liveStreamRepository.findById(liveStreamId)
                .orElseThrow(() -> new ResourceNotFoundException("Live stream not found"));

        if (liveStream.getStatus() != LiveStreamStatus.LIVE) {
            throw new BadRequestException("Live stream is not active");
        }

        liveStream.incrementViewers();
        liveStream = liveStreamRepository.save(liveStream);

        return mapToLiveStreamResponse(liveStream);
    }

    /**
     * Leave a live stream (decrement viewer count)
     */
    @Transactional
    public void leaveLiveStream(UUID liveStreamId) {
        LiveStream liveStream = liveStreamRepository.findById(liveStreamId).orElse(null);
        if (liveStream != null && liveStream.getStatus() == LiveStreamStatus.LIVE) {
            liveStream.decrementViewers();
            liveStreamRepository.save(liveStream);
        }
    }

    /**
     * Get active live streams
     */
    public List<LiveStreamResponse> getActiveLiveStreams(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return liveStreamRepository.findActiveLiveStreams(pageable).stream()
                .map(this::mapToLiveStreamResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get live stream by ID
     */
    public LiveStreamResponse getLiveStream(UUID liveStreamId) {
        LiveStream liveStream = liveStreamRepository.findById(liveStreamId)
                .orElseThrow(() -> new ResourceNotFoundException("Live stream not found"));
        return mapToLiveStreamResponse(liveStream);
    }

    /**
     * Get user's live stream history
     */
    public PagedResponse<LiveStreamResponse> getUserLiveStreams(UUID hostId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LiveStream> streams = liveStreamRepository.findByHostIdOrderByCreatedAtDesc(hostId, pageable);

        return PagedResponse.<LiveStreamResponse>builder()
                .content(streams.getContent().stream()
                        .map(this::mapToLiveStreamResponse)
                        .collect(Collectors.toList()))
                .page(streams.getNumber())
                .size(streams.getSize())
                .totalElements(streams.getTotalElements())
                .totalPages(streams.getTotalPages())
                .last(streams.isLast())
                .first(streams.isFirst())
                .build();
    }

    /**
     * Like a live stream
     */
    @Transactional
    public void likeLiveStream(UUID liveStreamId) {
        LiveStream liveStream = liveStreamRepository.findById(liveStreamId).orElse(null);
        if (liveStream != null) {
            liveStream.setLikesCount(liveStream.getLikesCount() + 1);
            liveStreamRepository.save(liveStream);
        }
    }

    private LiveStreamResponse mapToLiveStreamResponse(LiveStream ls) {
        return LiveStreamResponse.builder()
                .id(ls.getId())
                .host(LiveStreamResponse.HostSummary.builder()
                        .id(ls.getHost().getId())
                        .name(ls.getHost().getName())
                        .profilePic(ls.getHost().getProfilePic())
                        .isVerified(ls.getHost().getIsVerified())
                        .build())
                .title(ls.getTitle())
                .description(ls.getDescription())
                .thumbnailUrl(ls.getThumbnailUrl())
                .status(ls.getStatus())
                .roomId(ls.getRoomId())
                .streamUrl(hlsBaseUrl + ls.getStreamKey() + ".m3u8")
                .rtmpUrl(rtmpBaseUrl + ls.getStreamKey())
                .webrtcUrl(webrtcSignalUrl)
                .viewerCount(ls.getViewerCount())
                .peakViewers(ls.getPeakViewers())
                .totalGiftsValue(ls.getTotalGiftsValue())
                .likesCount(ls.getLikesCount())
                .commentsCount(ls.getCommentsCount())
                .scheduledAt(ls.getScheduledAt())
                .startedAt(ls.getStartedAt())
                .endedAt(ls.getEndedAt())
                .durationSeconds(ls.getDurationSeconds())
                .createdAt(ls.getCreatedAt())
                .build();
    }

    /**
     * Verify stream key for SRS callback
     */
    @Transactional
    public boolean verifyStreamKey(String streamKey) {
        LiveStream liveStream = liveStreamRepository.findByStreamKey(streamKey).orElse(null);

        if (liveStream == null || liveStream.getStatus() == LiveStreamStatus.ENDED
                || liveStream.getStatus() == LiveStreamStatus.CANCELLED) {
            return false;
        }

        // Update status to LIVE if it's scheduled
        if (liveStream.getStatus() == LiveStreamStatus.SCHEDULED) {
            liveStream.setStatus(LiveStreamStatus.LIVE);
            liveStream.setStartedAt(LocalDateTime.now());
            liveStreamRepository.save(liveStream);
        }

        return true;
    }

    /**
     * Set stream offline for SRS callback
     */
    @Transactional
    public void setStreamOffline(String streamKey) {
        LiveStream liveStream = liveStreamRepository.findByStreamKey(streamKey).orElse(null);

        if (liveStream != null && liveStream.getStatus() == LiveStreamStatus.LIVE) {
            liveStream.setStatus(LiveStreamStatus.ENDED);
            liveStream.setEndedAt(LocalDateTime.now());

            if (liveStream.getStartedAt() != null) {
                long seconds = Duration.between(liveStream.getStartedAt(), liveStream.getEndedAt()).getSeconds();
                liveStream.setDurationSeconds((int) seconds);
            }

            liveStreamRepository.save(liveStream);
            log.info("Live stream ended via webhook: {}", liveStream.getId());
        }
    }
}
