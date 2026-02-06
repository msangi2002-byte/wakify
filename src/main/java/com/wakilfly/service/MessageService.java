package com.wakilfly.service;

import com.wakilfly.dto.request.SendMessageRequest;
import com.wakilfly.dto.response.ConversationResponse;
import com.wakilfly.dto.response.MessageResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.PostResponse;
import com.wakilfly.model.Conversation;
import com.wakilfly.model.Message;
import com.wakilfly.model.User;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.repository.ConversationRepository;
import com.wakilfly.repository.MessageRepository;
import com.wakilfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public PagedResponse<ConversationResponse> getConversations(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Conversation> conversations = conversationRepository.findUserConversations(userId, pageable);

        return PagedResponse.<ConversationResponse>builder()
                .content(conversations.getContent().stream()
                        .map(conv -> mapToConversationResponse(conv, userId))
                        .collect(Collectors.toList()))
                .page(conversations.getNumber())
                .size(conversations.getSize())
                .totalElements(conversations.getTotalElements())
                .totalPages(conversations.getTotalPages())
                .last(conversations.isLast())
                .first(conversations.isFirst())
                .build();
    }

    public PagedResponse<MessageResponse> getMessages(UUID conversationId, UUID userId, int page, int size) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        // Verify user is part of conversation
        if (!conversation.getParticipantOne().getId().equals(userId) &&
                !conversation.getParticipantTwo().getId().equals(userId)) {
            throw new BadRequestException("You are not part of this conversation");
        }

        // Mark messages as read
        messageRepository.markAsRead(conversationId, userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageRepository
                .findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(conversationId, pageable);

        return PagedResponse.<MessageResponse>builder()
                .content(messages.getContent().stream()
                        .map(this::mapToMessageResponse)
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
    public MessageResponse sendMessage(UUID senderId, SendMessageRequest request) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId));
        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getRecipientId()));

        if (senderId.equals(request.getRecipientId())) {
            throw new BadRequestException("You cannot send a message to yourself");
        }

        // Find or create conversation
        Conversation conversation = conversationRepository.findByParticipants(senderId, request.getRecipientId())
                .orElseGet(() -> {
                    Conversation newConv = Conversation.builder()
                            .participantOne(sender)
                            .participantTwo(recipient)
                            .build();
                    return conversationRepository.save(newConv);
                });

        // Create message
        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .type(request.getType())
                .mediaUrl(request.getMediaUrl())
                .build();

        message = messageRepository.save(message);

        // Update conversation
        String preview = request.getContent();
        if (preview != null && preview.length() > 50) {
            preview = preview.substring(0, 50) + "...";
        }
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setLastMessagePreview(preview);
        conversationRepository.save(conversation);

        // TODO: Create notification for recipient
        log.info("Message sent from {} to {}", senderId, request.getRecipientId());

        return mapToMessageResponse(message);
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

    private ConversationResponse mapToConversationResponse(Conversation conversation, UUID currentUserId) {
        User otherUser = conversation.getParticipantOne().getId().equals(currentUserId)
                ? conversation.getParticipantTwo()
                : conversation.getParticipantOne();

        long unreadCount = messageRepository.countByConversationIdAndSenderIdNotAndIsReadFalse(
                conversation.getId(), currentUserId);

        return ConversationResponse.builder()
                .id(conversation.getId())
                .otherUser(PostResponse.UserSummary.builder()
                        .id(otherUser.getId())
                        .name(otherUser.getName())
                        .profilePic(otherUser.getProfilePic())
                        .build())
                .lastMessagePreview(conversation.getLastMessagePreview())
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(unreadCount)
                .isBuyerSellerChat(conversation.getIsBuyerSellerChat())
                .productId(conversation.getProductId())
                .build();
    }

    private MessageResponse mapToMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .sender(PostResponse.UserSummary.builder()
                        .id(message.getSender().getId())
                        .name(message.getSender().getName())
                        .profilePic(message.getSender().getProfilePic())
                        .build())
                .content(message.getContent())
                .type(message.getType())
                .mediaUrl(message.getMediaUrl())
                .isRead(message.getIsRead())
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
