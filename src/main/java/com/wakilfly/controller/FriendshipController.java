package com.wakilfly.controller;

import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.model.Friendship;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping("/request/{userId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendRequest(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID requesterId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        friendshipService.sendRequest(requesterId, userId);
        return ResponseEntity.ok(ApiResponse.success("Friend request sent", Map.of("status", "PENDING")));
    }

    @PostMapping("/accept/{requestId}")
    public ResponseEntity<ApiResponse<String>> acceptRequest(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        friendshipService.acceptRequest(userId, requestId);
        return ResponseEntity.ok(ApiResponse.success("Friend request accepted"));
    }

    @DeleteMapping("/decline/{requestId}")
    public ResponseEntity<ApiResponse<String>> declineRequest(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        friendshipService.declineRequest(userId, requestId);
        return ResponseEntity.ok(ApiResponse.success("Friend request declined"));
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<ApiResponse<String>> unfriend(
            @PathVariable UUID friendId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        friendshipService.unfriend(userId, friendId);
        return ResponseEntity.ok(ApiResponse.success("Unfriended successfully"));
    }

    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<Page<Friendship>>> getPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        Page<Friendship> requests = friendshipService.getPendingRequests(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Friendship>>> getFriends(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        Page<Friendship> friends = friendshipService.getFriends(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(friends));
    }
}
