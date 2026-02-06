package com.wakilfly.controller;

import com.wakilfly.dto.request.SendMessageRequest;
import com.wakilfly.dto.response.ApiResponse;
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
}
