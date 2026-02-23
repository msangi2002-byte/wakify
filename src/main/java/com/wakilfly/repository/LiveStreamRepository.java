package com.wakilfly.repository;

import com.wakilfly.model.LiveStream;
import com.wakilfly.model.LiveStreamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LiveStreamRepository extends JpaRepository<LiveStream, UUID> {

    Page<LiveStream> findByHostIdOrderByCreatedAtDesc(UUID hostId, Pageable pageable);

    Page<LiveStream> findByStatusOrderByViewerCountDesc(LiveStreamStatus status, Pageable pageable);

    @Query("SELECT ls FROM LiveStream ls WHERE ls.status = 'LIVE' ORDER BY ls.viewerCount DESC")
    List<LiveStream> findActiveLiveStreams(Pageable pageable);

    @Query("SELECT ls FROM LiveStream ls WHERE ls.status = 'LIVE' AND (:category IS NULL OR :category = 'all' OR ls.category = :category) ORDER BY ls.viewerCount DESC")
    List<LiveStream> findActiveLiveStreamsByCategory(@Param("category") String category, Pageable pageable);

    Optional<LiveStream> findByStreamKey(String streamKey);

    Optional<LiveStream> findByRoomId(String roomId);

    @Query("SELECT ls FROM LiveStream ls WHERE ls.host.id = :hostId AND ls.status = 'LIVE'")
    Optional<LiveStream> findActiveByHostId(@Param("hostId") UUID hostId);

    @Query("SELECT COUNT(ls) FROM LiveStream ls WHERE ls.status = 'LIVE'")
    long countActiveLiveStreams();
}
