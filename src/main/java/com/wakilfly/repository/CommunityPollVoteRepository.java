package com.wakilfly.repository;

import com.wakilfly.model.CommunityPollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityPollVoteRepository extends JpaRepository<CommunityPollVote, UUID> {

    Optional<CommunityPollVote> findByPollIdAndUserId(UUID pollId, UUID userId);

    boolean existsByPollIdAndUserId(UUID pollId, UUID userId);
}
