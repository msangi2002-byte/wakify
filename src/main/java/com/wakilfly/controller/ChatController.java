package com.wakilfly.controller;

import com.wakilfly.dto.request.SendMessageRequest;
import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.ConversationSummary;
import com.wakilfly.dto.response.MessageResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID senderId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        MessageResponse response = chatService.sendMessage(senderId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationSummary>>> getConversations(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        List<ConversationSummary> list = chatService.getConversationsList(userId, limit, includeArchived);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PutMapping("/{otherUserId}/read")
    public ResponseEntity<ApiResponse<Void>> markConversationRead(
            @PathVariable UUID otherUserId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        chatService.markConversationAsRead(userId, otherUserId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{otherUserId}")
    public ResponseEntity<ApiResponse<PagedResponse<MessageResponse>>> getConversation(
            @PathVariable UUID otherUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<MessageResponse> response = chatService.getConversation(userId, otherUserId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        long count = chatService.getTotalUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * Delete a message (soft delete). Only sender can delete.
     * DELETE /api/v1/messages/{messageId}
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable UUID messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        chatService.deleteMessage(messageId, userId);
        return ResponseEntity.ok(ApiResponse.success("Message deleted"));
    }

    /**
     * Archive a conversation (hide from main list).
     * POST /api/v1/messages/conversations/{otherUserId}/archive
     */
    @PostMapping("/conversations/{otherUserId}/archive")
    public ResponseEntity<ApiResponse<Void>> archiveConversation(
            @PathVariable UUID otherUserId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        chatService.archiveConversation(userId, otherUserId);
        return ResponseEntity.ok(ApiResponse.success("Conversation archived"));
    }

    /**
     * Unarchive a conversation.
     * DELETE /api/v1/messages/conversations/{otherUserId}/archive
     */
    @DeleteMapping("/conversations/{otherUserId}/archive")
    public ResponseEntity<ApiResponse<Void>> unarchiveConversation(
            @PathVariable UUID otherUserId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        chatService.unarchiveConversation(userId, otherUserId);
        return ResponseEntity.ok(ApiResponse.success("Conversation unarchived"));
    }
}
