package com.wakilfly.repository;

import com.wakilfly.model.JoinRequestStatus;
import com.wakilfly.model.LiveStreamJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LiveStreamJoinRequestRepository extends JpaRepository<LiveStreamJoinRequest, UUID> {

    List<LiveStreamJoinRequest> findByLiveStreamIdOrderByCreatedAtDesc(UUID liveStreamId);

    List<LiveStreamJoinRequest> findByLiveStreamIdAndStatusOrderByCreatedAtDesc(
            UUID liveStreamId, JoinRequestStatus status);

    Optional<LiveStreamJoinRequest> findByLiveStreamIdAndRequesterId(UUID liveStreamId, UUID requesterId);

    boolean existsByLiveStreamIdAndRequesterIdAndStatus(UUID liveStreamId, UUID requesterId, JoinRequestStatus status);

    Optional<LiveStreamJoinRequest> findByGuestStreamKey(String guestStreamKey);
}
