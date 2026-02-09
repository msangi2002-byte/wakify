package com.wakilfly.repository;

import com.wakilfly.model.User;
import com.wakilfly.model.UserBlock;
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
public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {

    Optional<UserBlock> findByBlockerAndBlocked(User blocker, User blocked);

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    void deleteByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    Page<UserBlock> findByBlockerIdOrderByCreatedAtDesc(UUID blockerId, Pageable pageable);

    /** IDs of users that this user has blocked */
    @Query("SELECT ub.blocked.id FROM UserBlock ub WHERE ub.blocker.id = :blockerId")
    List<UUID> findBlockedUserIdsByBlockerId(@Param("blockerId") UUID blockerId);

    /** IDs of users who have blocked this user */
    @Query("SELECT ub.blocker.id FROM UserBlock ub WHERE ub.blocked.id = :blockedId")
    List<UUID> findBlockerIdsByBlockedId(@Param("blockedId") UUID blockedId);
}
