package com.wakilfly.repository;

import com.wakilfly.model.LiveStreamComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LiveStreamCommentRepository extends JpaRepository<LiveStreamComment, UUID> {

    Page<LiveStreamComment> findByLiveStreamIdOrderByCreatedAtDesc(UUID liveStreamId, Pageable pageable);
}
