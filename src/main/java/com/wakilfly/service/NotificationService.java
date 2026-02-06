package com.wakilfly.service;

import com.wakilfly.dto.response.NotificationResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.*;
import com.wakilfly.repository.NotificationRepository;
import com.wakilfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Create a notification
     */
    @Transactional
    public Notification createNotification(UUID userId, NotificationType type, String title, String message,
            UUID referenceId, String referenceType, User actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .actorId(actor != null ? actor.getId() : null)
                .actorName(actor != null ? actor.getName() : null)
                .actorPic(actor != null ? actor.getProfilePic() : null)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification created for user {}: {}", userId, type);

        return notification;
    }

    /**
     * Send like notification
     */
    public void sendLikeNotification(Post post, User liker) {
        // Don't notify if user liked their own post
        if (post.getAuthor().getId().equals(liker.getId())) {
            return;
        }

        createNotification(
                post.getAuthor().getId(),
                NotificationType.LIKE,
                "New Like",
                liker.getName() + " liked your post",
                post.getId(),
                "POST",
                liker);
    }

    /**
     * Send comment notification
     */
    public void sendCommentNotification(Post post, User commenter, String commentPreview) {
        if (post.getAuthor().getId().equals(commenter.getId())) {
            return;
        }

        String message = commenter.getName() + " commented: " +
                (commentPreview.length() > 50 ? commentPreview.substring(0, 50) + "..." : commentPreview);

        createNotification(
                post.getAuthor().getId(),
                NotificationType.COMMENT,
                "New Comment",
                message,
                post.getId(),
                "POST",
                commenter);
    }

    /**
     * Send follow notification
     */
    public void sendFollowNotification(User followed, User follower) {
        createNotification(
                followed.getId(),
                NotificationType.FOLLOW,
                "New Follower",
                follower.getName() + " started following you",
                follower.getId(),
                "USER",
                follower);
    }

    /**
     * Send order notification
     */
    public void sendOrderNotification(Order order, NotificationType type, String title, String message) {
        // Notify buyer
        createNotification(
                order.getBuyer().getId(),
                type,
                title,
                message,
                order.getId(),
                "ORDER",
                null);

        // Notify seller
        createNotification(
                order.getBusiness().getOwner().getId(),
                type,
                title,
                message,
                order.getId(),
                "ORDER",
                null);
    }

    /**
     * Send subscription notification
     */
    public void sendSubscriptionNotification(UUID userId, NotificationType type, String title, String message) {
        createNotification(userId, type, title, message, null, "SUBSCRIPTION", null);
    }

    /**
     * Send message notification
     */
    public void sendMessageNotification(User recipient, User sender, String messagePreview) {
        String message = sender.getName() + ": " +
                (messagePreview.length() > 50 ? messagePreview.substring(0, 50) + "..." : messagePreview);

        createNotification(
                recipient.getId(),
                NotificationType.MESSAGE,
                "New Message",
                message,
                sender.getId(),
                "CONVERSATION",
                sender);
    }

    /**
     * Get user notifications
     */
    public PagedResponse<NotificationResponse> getNotifications(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

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

    /**
     * Get unread count
     */
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Mark single notification as read
     */
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification", "id", notificationId);
        }

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notification = notificationRepository.save(notification);

        return mapToResponse(notification);
    }

    /**
     * Mark all notifications as read
     */
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
        log.info("Marked all notifications as read for user {}", userId);
    }

    /**
     * Delete notification
     */
    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification", "id", notificationId);
        }

        notificationRepository.delete(notification);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .actorId(notification.getActorId())
                .actorName(notification.getActorName())
                .actorPic(notification.getActorPic())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
