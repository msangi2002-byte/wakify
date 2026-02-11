package com.wakilfly.service;

import com.wakilfly.dto.request.SendMessageRequest;
import com.wakilfly.dto.response.ConversationSummary;
import com.wakilfly.dto.response.MessageResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.Message;
import com.wakilfly.model.MessageType;
import com.wakilfly.model.User;
import com.wakilfly.repository.MessageRepository;
import com.wakilfly.repository.UserArchivedConversationRepository;
import com.wakilfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserArchivedConversationRepository archivedConversationRepository;

    @Transactional
    public MessageResponse sendMessage(UUID senderId, SendMessageRequest request) {
        String content = request.getContent() != null ? request.getContent().trim() : "";
        String mediaUrl = request.getMediaUrl() != null ? request.getMediaUrl().trim() : null;
        if (content.isEmpty() && (mediaUrl == null || mediaUrl.isEmpty())) {
            throw new BadRequestException("Content or mediaUrl is required");
        }
        if (content.isEmpty()) content = ""; // allow empty for voice/document

        User sender = userRepository.getReferenceById(senderId);
        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getRecipientId()));

        MessageType type = request.getType() != null ? request.getType() : MessageType.TEXT;
        Message message = Message.builder()
                .sender(sender)
                .recipient(recipient)
                .content(content)
                .mediaUrl(mediaUrl)
                .type(type)
                .isRead(false)
                .build();

        message = messageRepository.save(message);

        return mapToResponse(message, senderId);
    }

    public PagedResponse<MessageResponse> getConversation(UUID userId, UUID otherUserId, int page, int size) {
        User user1 = userRepository.getReferenceById(userId);
        User user2 = userRepository.getReferenceById(otherUserId);

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageRepository.findConversation(user1, user2, pageable);

        // Mark incoming messages as read (side effect in GET? Usually separate POST,
        // but convenient here)
        // Ideally separate markRead endpoint.

        return PagedResponse.<MessageResponse>builder()
                .content(messages.getContent().stream()
                        .map(m -> mapToResponse(m, userId))
                        .collect(Collectors.toList()))
                .page(messages.getNumber())
                .size(messages.getSize())
                .totalElements(messages.getTotalElements())
                .totalPages(messages.getTotalPages())
                .last(messages.isLast())
                .first(messages.isFirst())
                .build();
    }

    @Transactional
    public void markConversationAsRead(UUID userId, UUID otherUserId) {
        User recipient = userRepository.getReferenceById(userId);
        User sender = userRepository.getReferenceById(otherUserId);
        messageRepository.markAsReadByRecipientAndSender(recipient, sender);
    }

    public long getTotalUnreadCount(UUID userId) {
        User user = userRepository.getReferenceById(userId);
        return messageRepository.countByRecipientAndIsReadFalse(user);
    }

    public List<ConversationSummary> getConversationsList(UUID userId, int limit, boolean includeArchived) {
        User user = userRepository.getReferenceById(userId);
        Pageable pageable = PageRequest.of(0, 500);
        Page<Message> messages = messageRepository.findRecentForUser(user, pageable);

        Set<UUID> archivedOtherIds = includeArchived ? Collections.emptySet() : getArchivedOtherUserIds(userId);

        Map<UUID, ConversationSummary> byOther = new LinkedHashMap<>();
        for (Message m : messages.getContent()) {
            User other = m.getSender().getId().equals(userId) ? m.getRecipient() : m.getSender();
            if (byOther.containsKey(other.getId())) continue;
            if (!includeArchived && archivedOtherIds.contains(other.getId())) continue;
            long unread = messageRepository.countBySenderAndRecipientAndIsReadFalse(other, user);
            boolean archived = archivedConversationRepository.existsByUserIdAndOtherUserId(userId, other.getId());
            byOther.put(other.getId(), ConversationSummary.builder()
                    .otherUserId(other.getId())
                    .otherUserName(other.getName())
                    .otherUserProfilePic(other.getProfilePic())
                    .lastMessageContent(m.getContent())
                    .lastMessageAt(m.getCreatedAt())
                    .unreadCount(unread)
                    .archived(archived)
                    .build());
            if (byOther.size() >= limit) break;
        }
        return new ArrayList<>(byOther.values());
    }

    private Set<UUID> getArchivedOtherUserIds(UUID userId) {
        return archivedConversationRepository.findByUserId(userId).stream()
                .map(a -> a.getOtherUser().getId())
                .collect(Collectors.toSet());
    }

    @Transactional
    public void archiveConversation(UUID userId, UUID otherUserId) {
        User user = userRepository.getReferenceById(userId);
        User other = userRepository.getReferenceById(otherUserId);
        if (archivedConversationRepository.existsByUserIdAndOtherUserId(userId, otherUserId)) return;
        archivedConversationRepository.save(com.wakilfly.model.UserArchivedConversation.builder()
                .user(user)
                .otherUser(other)
                .build());
    }

    @Transactional
    public void unarchiveConversation(UUID userId, UUID otherUserId) {
        archivedConversationRepository.deleteByUserIdAndOtherUserId(userId, otherUserId);
    }

    @Transactional
    public void deleteMessage(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));
        if (!message.getSender().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own messages");
        }
        message.setIsDeleted(true);
        messageRepository.save(message);
    }

    private MessageResponse mapToResponse(Message m, UUID currentUserId) {
        return MessageResponse.builder()
                .id(m.getId())
                .content(m.getContent())
                .mediaUrl(m.getMediaUrl())
                .type(m.getType() != null ? m.getType().name() : MessageType.TEXT.name())
                .createdAt(m.getCreatedAt())
                .isRead(m.getIsRead())
                .senderId(m.getSender().getId())
                .senderName(m.getSender().getName())
                .senderProfilePic(m.getSender().getProfilePic())
                .isMe(m.getSender().getId().equals(currentUserId))
                .build();
    }
}
