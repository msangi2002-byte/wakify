package com.wakilfly.controller;

import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.NotificationResponse;
import com.wakilfly.dto.response.NotificationSettingsResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.UserResponse;
import com.wakilfly.model.NotificationType;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CustomUserDetailsService userDetailsService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<NotificationResponse> response = notificationService.getUserNotifications(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> getSettings(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        NotificationSettingsResponse settings = notificationService.getNotificationSettings(userId);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<Void>> updateSettings(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        String typeStr = body != null && body.containsKey("type") ? String.valueOf(body.get("type")) : null;
        Boolean enabled = body != null && body.get("enabled") != null
                ? Boolean.TRUE.equals(body.get("enabled")) : null;
        if (typeStr == null || typeStr.isBlank() || enabled == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("type and enabled are required"));
        }
        NotificationType type;
        try {
            type = NotificationType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid notification type"));
        }
        notificationService.updateNotificationSetting(userId, type, enabled);
        return ResponseEntity.ok(ApiResponse.success("Settings updated"));
    }

    @PostMapping("/mute/{userId}")
    public ResponseEntity<ApiResponse<Void>> muteUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        notificationService.muteUserNotifications(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("Notifications from this user are muted"));
    }

    @DeleteMapping("/mute/{userId}")
    public ResponseEntity<ApiResponse<Void>> unmuteUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        notificationService.unmuteUserNotifications(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("Notifications unmuted"));
    }

    @GetMapping("/muted")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getMutedUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<UserResponse> muted = notificationService.getMutedUsers(currentUserId, page, size);
        return ResponseEntity.ok(ApiResponse.success(muted));
    }
}
