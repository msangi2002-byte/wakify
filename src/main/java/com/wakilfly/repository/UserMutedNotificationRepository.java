package com.wakilfly.repository;

import com.wakilfly.model.UserMutedNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserMutedNotificationRepository extends JpaRepository<UserMutedNotification, UUID> {

    boolean existsByUserIdAndMutedUserId(UUID userId, UUID mutedUserId);

    void deleteByUserIdAndMutedUserId(UUID userId, UUID mutedUserId);

    Page<UserMutedNotification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
