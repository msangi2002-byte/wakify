package com.wakilfly.repository;

import com.wakilfly.model.CommunityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface CommunityEventRepository extends JpaRepository<CommunityEvent, UUID> {

    Page<CommunityEvent> findByCommunityIdOrderByStartTimeAsc(UUID communityId, Pageable pageable);

    Page<CommunityEvent> findByCommunityIdAndStartTimeAfterOrderByStartTimeAsc(UUID communityId, LocalDateTime after, Pageable pageable);
}
