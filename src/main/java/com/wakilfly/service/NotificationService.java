package com.wakilfly.service;

import com.wakilfly.dto.response.NotificationResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.model.Notification;
import com.wakilfly.model.NotificationType;
import com.wakilfly.model.User;
import com.wakilfly.model.UserMutedNotification;
import com.wakilfly.model.UserNotificationSettings;
import com.wakilfly.repository.NotificationRepository;
import com.wakilfly.repository.UserMutedNotificationRepository;
import com.wakilfly.repository.UserNotificationSettingsRepository;
import com.wakilfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static com.wakilfly.model.NotificationType.LIVE_STARTED;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserNotificationSettingsRepository notificationSettingsRepository;
    private final UserMutedNotificationRepository mutedNotificationRepository;

    /**
     * Notify all followers of the host that they started a live stream.
     */
    @Transactional
    public void notifyLiveStarted(User host, UUID liveStreamId) {
        Page<User> followers = userRepository.findFollowers(host.getId(), PageRequest.of(0, 500));
        for (User recipient : followers.getContent()) {
            sendNotification(recipient, host, LIVE_STARTED, liveStreamId,
                    host.getName() + " started a live stream");
        }
    }

    @Transactional
    public void sendNotification(User recipient, User actor, NotificationType type, UUID entityId, String message) {
        // Don't notify self
        if (actor != null && recipient.getId().equals(actor.getId())) {
            return;
        }
        // Skip if recipient muted this actor
        if (actor != null && mutedNotificationRepository.existsByUserIdAndMutedUserId(recipient.getId(), actor.getId())) {
            return;
        }
        // Skip if recipient disabled this notification type
        boolean typeEnabled = notificationSettingsRepository.findByUserIdAndType(recipient.getId(), type)
                .map(s -> Boolean.TRUE.equals(s.getEnabled()))
                .orElse(true);
        if (!typeEnabled) return;

        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .entityId(entityId)
                .message(message)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    public PagedResponse<NotificationResponse> getUserNotifications(UUID userId, int page, int size, NotificationType typeFilter) {
        User user = userRepository.getReferenceById(userId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = typeFilter != null
                ? notificationRepository.findByRecipientAndTypeOrderByCreatedAtDesc(user, typeFilter, pageable)
                : notificationRepository.findByRecipientOrderByCreatedAtDesc(user, pageable);

        return PagedResponse.<NotificationResponse>builder()
                .content(notifications.getContent().stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()))
                .page(notifications.getNumber())
                .size(notifications.getSize())
                .totalElements(notifications.getTotalElements())
                .totalPages(notifications.getTotalPages())
                .last(notifications.isLast())
                .first(notifications.isFirst())
                .build();
    }

    public long getUnreadCount(UUID userId) {
        User user = userRepository.getReferenceById(userId);
        return notificationRepository.countByRecipientAndIsReadFalse(user);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    public com.wakilfly.dto.response.NotificationSettingsResponse getNotificationSettings(UUID userId) {
        Map<String, Boolean> byType = new HashMap<>();
        for (NotificationType t : NotificationType.values()) {
            byType.put(t.name(), notificationSettingsRepository.findByUserIdAndType(userId, t)
                    .map(s -> Boolean.TRUE.equals(s.getEnabled()))
                    .orElse(true));
        }
        return com.wakilfly.dto.response.NotificationSettingsResponse.builder()
                .byType(byType)
                .build();
    }

    @Transactional
    public void updateNotificationSetting(UUID userId, NotificationType type, boolean enabled) {
        User user = userRepository.getReferenceById(userId);
        UserNotificationSettings s = notificationSettingsRepository.findByUserIdAndType(userId, type)
                .orElse(UserNotificationSettings.builder().user(user).type(type).enabled(true).build());
        s.setEnabled(enabled);
        notificationSettingsRepository.save(s);
    }

    @Transactional
    public void muteUserNotifications(UUID userId, UUID mutedUserId) {
        if (userId.equals(mutedUserId)) return;
        User user = userRepository.getReferenceById(userId);
        User muted = userRepository.getReferenceById(mutedUserId);
        if (mutedNotificationRepository.existsByUserIdAndMutedUserId(userId, mutedUserId)) return;
        mutedNotificationRepository.save(UserMutedNotification.builder().user(user).mutedUser(muted).build());
    }

    @Transactional
    public void unmuteUserNotifications(UUID userId, UUID mutedUserId) {
        mutedNotificationRepository.deleteByUserIdAndMutedUserId(userId, mutedUserId);
    }

    public PagedResponse<com.wakilfly.dto.response.UserResponse> getMutedUsers(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserMutedNotification> p = mutedNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<com.wakilfly.dto.response.UserResponse> content = p.getContent().stream()
                .map(m -> com.wakilfly.dto.response.UserResponse.builder()
                        .id(m.getMutedUser().getId())
                        .name(m.getMutedUser().getName())
                        .profilePic(m.getMutedUser().getProfilePic())
                        .build())
                .collect(Collectors.toList());
        return PagedResponse.<com.wakilfly.dto.response.UserResponse>builder()
                .content(content)
                .page(p.getNumber())
                .size(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .last(p.isLast())
                .first(p.isFirst())
                .build();
    }

    private NotificationResponse mapToResponse(Notification n) {
        NotificationResponse.ActorSummary actorSummary = null;
        if (n.getActor() != null) {
            actorSummary = NotificationResponse.ActorSummary.builder()
                    .id(n.getActor().getId())
                    .name(n.getActor().getName())
                    .profilePic(n.getActor().getProfilePic())
                    .build();
        }

        return NotificationResponse.builder()
                .id(n.getId())
                .actor(actorSummary)
                .message(n.getMessage())
                .type(n.getType())
                .entityId(n.getEntityId())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
