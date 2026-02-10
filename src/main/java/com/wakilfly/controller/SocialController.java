package com.wakilfly.controller;

import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.UserResponse;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/social")
@RequiredArgsConstructor
public class SocialController {

    private final UserService userService;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping("/follow/{userId}")
    public ResponseEntity<ApiResponse<Void>> followUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        userService.followUser(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("You are now following this user"));
    }

    @DeleteMapping("/follow/{userId}")
    public ResponseEntity<ApiResponse<Void>> unfollowUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        userService.unfollowUser(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("You have unfollowed this user"));
    }

    @GetMapping("/followers/{userId}")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getFollowers(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = null;
        if (userDetails != null) {
            currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        }
        PagedResponse<UserResponse> followers = userService.getFollowers(userId, page, size, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(followers));
    }

    @GetMapping("/following/{userId}")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getFollowing(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = null;
        if (userDetails != null) {
            currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        }
        PagedResponse<UserResponse> following = userService.getFollowing(userId, page, size, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(following));
    }

    /** Mutual follows: users I follow AND who follow me back (malafiki). For Messages page. */
    @GetMapping("/mutual-follows")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getMutualFollows(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<UserResponse> mutual = userService.getMutualFollows(currentUserId, page, size);
        return ResponseEntity.ok(ApiResponse.success(mutual));
    }
}
