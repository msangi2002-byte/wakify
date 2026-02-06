package com.wakilfly.service;

import com.wakilfly.dto.request.SendMessageRequest;
import com.wakilfly.dto.response.MessageResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.model.Message;
import com.wakilfly.model.User;
import com.wakilfly.repository.MessageRepository;
import com.wakilfly.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatService chatService;

    private User sender;
    private User recipient;
    private UUID senderId;
    private UUID recipientId;

    @BeforeEach
    void setUp() {
        senderId = UUID.randomUUID();
        recipientId = UUID.randomUUID();

        sender = User.builder().id(senderId).name("Sender").build();
        recipient = User.builder().id(recipientId).name("Recipient").build();
    }

    @Test
    void sendMessage_ShouldSaveMessage() {
        // Arrange
        SendMessageRequest request = new SendMessageRequest();
        request.setRecipientId(recipientId);
        request.setContent("Hello World");

        when(userRepository.getReferenceById(senderId)).thenReturn(sender);
        when(userRepository.findById(recipientId)).thenReturn(Optional.of(recipient));

        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(UUID.randomUUID());
            msg.setCreatedAt(LocalDateTime.now());
            return msg;
        });

        // Act
        MessageResponse response = chatService.sendMessage(senderId, request);

        // Assert
        assertNotNull(response);
        assertEquals("Hello World", response.getContent());
        assertEquals(senderId, response.getSenderId());
        assertFalse(response.getIsRead());
        assertTrue(response.getIsMe());

        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    void getConversation_ShouldReturnMessages() {
        // Arrange
        Message msg = Message.builder()
                .id(UUID.randomUUID())
                .sender(sender)
                .recipient(recipient)
                .content("Hi")
                .createdAt(LocalDateTime.now())
                .build();

        Page<Message> page = new PageImpl<>(Collections.singletonList(msg));

        when(userRepository.getReferenceById(senderId)).thenReturn(sender);
        when(userRepository.getReferenceById(recipientId)).thenReturn(recipient);
        when(messageRepository.findConversation(any(User.class), any(User.class), any(Pageable.class)))
                .thenReturn(page);

        // Act
        PagedResponse<MessageResponse> response = chatService.getConversation(senderId, recipientId, 0, 10);

        // Assert
        assertEquals(1, response.getContent().size());
        assertEquals("Hi", response.getContent().get(0).getContent());
    }
}
