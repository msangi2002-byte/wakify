package com.wakilfly.controller;

import com.wakilfly.dto.request.SendMessageRequest;
import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.ConversationResponse;
import com.wakilfly.dto.response.MessageResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final CustomUserDetailsService userDetailsService;

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<PagedResponse<ConversationResponse>>> getConversations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<ConversationResponse> conversations = messageService.getConversations(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<ApiResponse<PagedResponse<MessageResponse>>> getMessages(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<MessageResponse> messages = messageService.getMessages(conversationId, userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SendMessageRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        MessageResponse message = messageService.sendMessage(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(message));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable UUID messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        messageService.deleteMessage(messageId, userId);
        return ResponseEntity.ok(ApiResponse.success("Message deleted successfully"));
    }
}
