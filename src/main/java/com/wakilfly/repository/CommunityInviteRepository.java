package com.wakilfly.repository;

import com.wakilfly.model.CommunityInvite;
import com.wakilfly.model.CommunityInviteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityInviteRepository extends JpaRepository<CommunityInvite, UUID> {

    Optional<CommunityInvite> findByCommunityIdAndInviteeId(UUID communityId, UUID inviteeId);

    boolean existsByCommunityIdAndInviteeIdAndStatus(UUID communityId, UUID inviteeId, CommunityInviteStatus status);

    Page<CommunityInvite> findByInviteeIdAndStatus(UUID inviteeId, CommunityInviteStatus status, Pageable pageable);

    Page<CommunityInvite> findByCommunityId(UUID communityId, Pageable pageable);
}
