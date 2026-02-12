package com.wakilfly.controller;

import com.wakilfly.dto.request.CreateBusinessRequestRequest;
import com.wakilfly.dto.request.UpdateProfileRequest;
import com.wakilfly.dto.request.UploadContactsRequest;
import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.AuthEventResponse;
import com.wakilfly.dto.response.BusinessRequestResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.UserResponse;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.AuthEventService;
import com.wakilfly.service.BusinessRequestService;
import com.wakilfly.service.PeopleYouMayKnowService;
import com.wakilfly.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthEventService authEventService;
    private final PeopleYouMayKnowService peopleYouMayKnowService;
    private final BusinessRequestService businessRequestService;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping("/me/activity")
    public ResponseEntity<ApiResponse<String>> recordActivity(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        userService.updateLastSeen(userId);
        return ResponseEntity.ok(ApiResponse.success("Activity recorded"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * Login activity (where you're logged in) – like Facebook/Instagram.
     * Shows registration + login events with IP, device, browser, OS.
     * GET /api/v1/users/me/login-activity
     */
    @GetMapping("/me/login-activity")
    public ResponseEntity<ApiResponse<PagedResponse<AuthEventResponse>>> getLoginActivity(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<AuthEventResponse> activity = authEventService.getLoginActivity(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(activity));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        UserResponse user = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", user));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("file") MultipartFile file) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        UserResponse user = userService.uploadProfilePic(userId, file);
        return ResponseEntity.ok(ApiResponse.success("Profile picture updated", user));
    }

    @PostMapping(value = "/me/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> uploadCover(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("file") MultipartFile file) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        UserResponse user = userService.uploadCoverPic(userId, file);
        return ResponseEntity.ok(ApiResponse.success("Cover picture updated", user));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = null;
        if (userDetails != null) {
            currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        }
        UserResponse user = userService.getUserProfile(userId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> searchUsers(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<UserResponse> users = userService.searchUsers(q == null ? "" : q.trim(), page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /** Discover / people you may know: same region & country (from your profile/register). Auth required. */
    @GetMapping("/suggested")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getSuggestedUsers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<UserResponse> users = userService.getSuggestedUsers(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /** People nearby: same country; ordered by proximity (same city first, then region). Uses profile/register location. Auth required. */
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getNearbyUsers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<UserResponse> users = userService.getNearbyUsers(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * Upload contact list (phones/emails) for "People You May Know". Stored hashed.
     * Returns Wakify users whose phone or email is in your contacts (excludes self & already following).
     * POST /api/v1/users/me/contacts
     */
    /**
     * Request to become a business (user selects an agent; agent sees request in their dashboard).
     * POST /api/v1/users/me/business-requests
     */
    @PostMapping("/me/business-requests")
    public ResponseEntity<ApiResponse<BusinessRequestResponse>> createBusinessRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateBusinessRequestRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        BusinessRequestResponse created = businessRequestService.create(userId, request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success("Request submitted. Your selected agent will contact you.", created));
    }

    @PostMapping("/me/contacts")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> uploadContacts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UploadContactsRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<UserResponse> suggestions = peopleYouMayKnowService.uploadContactsAndGetSuggestions(userId, request);
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    /**
     * People You May Know: unified score = contact match + location + mutual friends + interests. Sorted by score desc.
     * GET /api/v1/users/people-you-may-know
     */
    @GetMapping("/people-you-may-know")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getPeopleYouMayKnow(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<UserResponse> users = peopleYouMayKnowService.getPeopleYouMayKnow(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    // ==================== BLOCK USER ====================

    /**
     * Block a user (they won't see your posts/stories in their feed; you won't see theirs).
     * POST /api/v1/users/{userId}/block
     */
    @PostMapping("/{userId}/block")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        userService.blockUser(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("User blocked"));
    }

    /**
     * Unblock a user.
     * DELETE /api/v1/users/{userId}/block
     */
    @DeleteMapping("/{userId}/block")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        userService.unblockUser(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("User unblocked"));
    }

    /**
     * List users you have blocked.
     * GET /api/v1/users/me/blocked
     */
    @GetMapping("/me/blocked")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getBlockedUsers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<UserResponse> blocked = userService.getBlockedUsers(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(blocked));
    }

    // ==================== RESTRICTED LIST ====================

    /**
     * Add user to restricted list (they only see your public content).
     * POST /api/v1/users/{userId}/restrict
     */
    @PostMapping("/{userId}/restrict")
    public ResponseEntity<ApiResponse<Void>> restrictUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        userService.addToRestrictedList(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("User added to restricted list"));
    }

    /**
     * Remove user from restricted list.
     * DELETE /api/v1/users/{userId}/restrict
     */
    @DeleteMapping("/{userId}/restrict")
    public ResponseEntity<ApiResponse<Void>> unrestrictUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        userService.removeFromRestrictedList(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("User removed from restricted list"));
    }

    /**
     * List users you have restricted.
     * GET /api/v1/users/me/restricted
     */
    @GetMapping("/me/restricted")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getRestrictedUsers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<UserResponse> restricted = userService.getRestrictedList(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(restricted));
    }
}
