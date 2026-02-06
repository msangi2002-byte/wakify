package com.wakilfly.service;

import com.wakilfly.dto.response.NotificationResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.model.Notification;
import com.wakilfly.model.NotificationType;
import com.wakilfly.model.User;
import com.wakilfly.repository.NotificationRepository;
import com.wakilfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void sendNotification(User recipient, User actor, NotificationType type, UUID entityId, String message) {
        // Don't notify self
        if (actor != null && recipient.getId().equals(actor.getId())) {
            return;
        }

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

    public PagedResponse<NotificationResponse> getUserNotifications(UUID userId, int page, int size) {
        User user = userRepository.getReferenceById(userId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository.findByRecipientOrderByCreatedAtDesc(user, pageable);

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
