package com.wakilfly.service;

import com.wakilfly.dto.response.JoinRequestResponse;
import com.wakilfly.dto.response.LiveStreamCommentResponse;
import com.wakilfly.dto.response.LiveStreamResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.*;
import com.wakilfly.repository.LiveStreamCommentRepository;
import com.wakilfly.repository.LiveStreamJoinRequestRepository;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveStreamService {

    private final LiveStreamRepository liveStreamRepository;
    private final LiveStreamJoinRequestRepository joinRequestRepository;
    private final LiveStreamCommentRepository liveStreamCommentRepository;
    private final UserRepository userRepository;

    @Value("${streaming.rtmp-url}")
    private String rtmpBaseUrl;

    @Value("${streaming.hls-url}")
    private String hlsBaseUrl;

    @Value("${streaming.hls-path-suffix:.m3u8}")
    private String hlsPathSuffix;

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

    // ---------- Join request (guest) ----------

    /**
     * Viewer requests to join the live stream as guest.
     */
    @Transactional
    public JoinRequestResponse requestToJoinLive(UUID liveStreamId, UUID requesterId) {
        LiveStream liveStream = liveStreamRepository.findById(liveStreamId)
                .orElseThrow(() -> new ResourceNotFoundException("Live stream not found"));

        if (liveStream.getStatus() != LiveStreamStatus.LIVE) {
            throw new BadRequestException("Live stream is not active");
        }

        if (liveStream.getHost().getId().equals(requesterId)) {
            throw new BadRequestException("Host cannot request to join own stream");
        }

        if (joinRequestRepository.existsByLiveStreamIdAndRequesterIdAndStatus(
                liveStreamId, requesterId, JoinRequestStatus.PENDING)) {
            throw new BadRequestException("You already have a pending request for this stream");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LiveStreamJoinRequest request = LiveStreamJoinRequest.builder()
                .liveStream(liveStream)
                .requester(requester)
                .status(JoinRequestStatus.PENDING)
                .build();
        request = joinRequestRepository.save(request);
        log.info("Join request created: liveStream={}, requester={}", liveStreamId, requesterId);
        return mapToJoinRequestResponse(request);
    }

    /**
     * Host gets join requests for their live stream (default: pending only).
     */
    public List<JoinRequestResponse> getJoinRequests(UUID liveStreamId, UUID hostId, boolean pendingOnly) {
        LiveStream liveStream = liveStreamRepository.findById(liveStreamId)
                .orElseThrow(() -> new ResourceNotFoundException("Live stream not found"));

        if (!liveStream.getHost().getId().equals(hostId)) {
            throw new BadRequestException("You are not the host of this stream");
        }

        List<LiveStreamJoinRequest> list = pendingOnly
                ? joinRequestRepository.findByLiveStreamIdAndStatusOrderByCreatedAtDesc(
                        liveStreamId, JoinRequestStatus.PENDING)
                : joinRequestRepository.findByLiveStreamIdOrderByCreatedAtDesc(liveStreamId);

        return list.stream().map(this::mapToJoinRequestResponse).collect(Collectors.toList());
    }

    /**
     * Host accepts a join request (guest can be invited to publish in phase 2).
     */
    @Transactional
    public JoinRequestResponse acceptJoinRequest(UUID requestId, UUID hostId) {
        LiveStreamJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Join request not found"));

        if (!request.getLiveStream().getHost().getId().equals(hostId)) {
            throw new BadRequestException("You are not the host of this stream");
        }

        if (request.getStatus() != JoinRequestStatus.PENDING) {
            throw new BadRequestException("Request already responded to");
        }

        request.setStatus(JoinRequestStatus.ACCEPTED);
        request.setHostRespondedAt(LocalDateTime.now());
        request = joinRequestRepository.save(request);
        log.info("Join request accepted: {}", requestId);
        return mapToJoinRequestResponse(request);
    }

    /**
     * Host rejects a join request.
     */
    @Transactional
    public JoinRequestResponse rejectJoinRequest(UUID requestId, UUID hostId) {
        LiveStreamJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Join request not found"));

        if (!request.getLiveStream().getHost().getId().equals(hostId)) {
            throw new BadRequestException("You are not the host of this stream");
        }

        if (request.getStatus() != JoinRequestStatus.PENDING) {
            throw new BadRequestException("Request already responded to");
        }

        request.setStatus(JoinRequestStatus.REJECTED);
        request.setHostRespondedAt(LocalDateTime.now());
        request = joinRequestRepository.save(request);
        log.info("Join request rejected: {}", requestId);
        return mapToJoinRequestResponse(request);
    }

    /**
     * Get current user's join request for a live (viewer only). When status is ACCEPTED,
     * response includes guestStreamKey so the guest can publish and appear on the same live.
     */
    public Optional<JoinRequestResponse> getMyJoinRequestForLive(UUID liveStreamId, UUID requesterId) {
        LiveStream liveStream = liveStreamRepository.findById(liveStreamId).orElse(null);
        if (liveStream == null) return Optional.empty();
        return joinRequestRepository.findByLiveStreamIdAndRequesterId(liveStreamId, requesterId)
                .map(req -> mapToJoinRequestResponseWithGuestKey(req, liveStream));
    }

    private JoinRequestResponse mapToJoinRequestResponseWithGuestKey(LiveStreamJoinRequest req, LiveStream liveStream) {
        JoinRequestResponse.JoinRequestResponseBuilder b = JoinRequestResponse.builder()
                .id(req.getId())
                .liveStreamId(req.getLiveStream().getId())
                .requester(JoinRequestResponse.RequesterSummary.builder()
                        .id(req.getRequester().getId())
                        .name(req.getRequester().getName())
                        .profilePic(req.getRequester().getProfilePic())
                        .isVerified(req.getRequester().getIsVerified())
                        .build())
                .status(req.getStatus())
                .hostRespondedAt(req.getHostRespondedAt())
                .createdAt(req.getCreatedAt());
        if (req.getStatus() == JoinRequestStatus.ACCEPTED && liveStream.getStreamKey() != null) {
            b.guestStreamKey(liveStream.getStreamKey());
        }
        return b.build();
    }

    // ---------- Live comments ----------

    /**
     * Send a comment on a live stream. Visible to all viewers; increments commentsCount.
     */
    @Transactional
    public LiveStreamCommentResponse addComment(UUID liveStreamId, UUID authorId, String content) {
        LiveStream liveStream = liveStreamRepository.findById(liveStreamId)
                .orElseThrow(() -> new ResourceNotFoundException("Live stream not found"));
        if (liveStream.getStatus() != LiveStreamStatus.LIVE) {
            throw new BadRequestException("Can only comment on an active live stream");
        }
        if (content == null || content.isBlank() || content.length() > 500) {
            throw new BadRequestException("Comment must be 1-500 characters");
        }
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LiveStreamComment comment = LiveStreamComment.builder()
                .liveStream(liveStream)
                .author(author)
                .content(content.trim())
                .build();
        comment = liveStreamCommentRepository.save(comment);

        liveStream.setCommentsCount((liveStream.getCommentsCount() == null ? 0 : liveStream.getCommentsCount()) + 1);
        liveStreamRepository.save(liveStream);

        return mapToCommentResponse(comment);
    }

    /**
     * Get comments for a live stream (paginated, newest first).
     */
    public PagedResponse<LiveStreamCommentResponse> getComments(UUID liveStreamId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LiveStreamComment> comments = liveStreamCommentRepository.findByLiveStreamIdOrderByCreatedAtDesc(liveStreamId, pageable);
        return PagedResponse.<LiveStreamCommentResponse>builder()
                .content(comments.getContent().stream().map(this::mapToCommentResponse).collect(Collectors.toList()))
                .page(comments.getNumber())
                .size(comments.getSize())
                .totalElements(comments.getTotalElements())
                .totalPages(comments.getTotalPages())
                .last(comments.isLast())
                .first(comments.isFirst())
                .build();
    }

    private LiveStreamCommentResponse mapToCommentResponse(LiveStreamComment c) {
        User a = c.getAuthor();
        return LiveStreamCommentResponse.builder()
                .id(c.getId())
                .authorId(a.getId())
                .authorName(a.getName())
                .authorProfilePic(a.getProfilePic())
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private JoinRequestResponse mapToJoinRequestResponse(LiveStreamJoinRequest req) {
        return mapToJoinRequestResponseWithGuestKey(req, req.getLiveStream());
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
                .streamUrl(hlsBaseUrl + ls.getStreamKey() + hlsPathSuffix)
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
