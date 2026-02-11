package com.wakilfly.repository;

import com.wakilfly.model.NotificationType;
import com.wakilfly.model.UserNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserNotificationSettingsRepository extends JpaRepository<UserNotificationSettings, UUID> {

    Optional<UserNotificationSettings> findByUserIdAndType(UUID userId, NotificationType type);

    List<UserNotificationSettings> findByUserId(UUID userId);
}
