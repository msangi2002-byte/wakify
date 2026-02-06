package com.wakilfly.repository;

import com.wakilfly.model.CommunityMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityMemberRepository extends JpaRepository<CommunityMember, UUID> {

    // Find groups a user has joined
    Page<CommunityMember> findByUserId(UUID userId, Pageable pageable);

    // Check membership
    boolean existsByCommunityIdAndUserId(UUID communityId, UUID userId);

    // Get specific membership (e.g. to check Role or Ban status)
    Optional<CommunityMember> findByCommunityIdAndUserId(UUID communityId, UUID userId);

    // Count members
    long countByCommunityId(UUID communityId);
}
