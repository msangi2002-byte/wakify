package com.wakilfly.repository;

import com.wakilfly.model.CommunityPoll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommunityPollRepository extends JpaRepository<CommunityPoll, UUID> {

    Page<CommunityPoll> findByCommunityIdOrderByCreatedAtDesc(UUID communityId, Pageable pageable);
}
