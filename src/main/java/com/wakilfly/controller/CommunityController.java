package com.wakilfly.controller;

import com.wakilfly.dto.request.CreateCommunityRequest;
import com.wakilfly.dto.request.CreateCommunityEventRequest;
import com.wakilfly.dto.request.CreateCommunityPollRequest;
import com.wakilfly.dto.request.CommunityInviteRequest;
import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.CommunityEventResponse;
import com.wakilfly.dto.response.CommunityInviteResponse;
import com.wakilfly.dto.response.CommunityPollResponse;
import com.wakilfly.dto.response.CommunityResponse;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CommunityResponse>> createCommunity(
            @ModelAttribute @Valid CreateCommunityRequest request,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        CommunityResponse response = communityService.createCommunity(userId, request, coverImage);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<String>> joinCommunity(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        communityService.joinCommunity(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Joined community successfully"));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<String>> leaveCommunity(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        communityService.leaveCommunity(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Left community successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CommunityResponse>>> getAllCommunities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "membersCount") String sortBy,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        Page<CommunityResponse> communities = communityService.getAllCommunities(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy)));
        return ResponseEntity.ok(ApiResponse.success(communities));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<CommunityResponse>>> getMyCommunities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        Page<CommunityResponse> communities = communityService.getMyCommunities(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(communities));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CommunityResponse>> getCommunity(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        CommunityResponse community = communityService.getCommunityById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(community));
    }

    @PutMapping("/{id}/settings")
    public ResponseEntity<ApiResponse<CommunityResponse>> updateCommunitySettings(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, Boolean> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        Boolean allowMemberPosts = body != null && body.containsKey("allowMemberPosts")
                ? body.get("allowMemberPosts")
                : null;
        if (allowMemberPosts == null) {
            allowMemberPosts = true;
        }
        CommunityResponse community = communityService.updateSettings(id, userId, allowMemberPosts);
        return ResponseEntity.ok(ApiResponse.success(community));
    }

    /**
     * Pin a post in the group (admin only). Pinned posts show first in group feed.
     * POST /api/v1/communities/{id}/posts/{postId}/pin
     */
    @PostMapping("/{id}/posts/{postId}/pin")
    public ResponseEntity<ApiResponse<String>> pinPost(
            @PathVariable UUID id,
            @PathVariable UUID postId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        communityService.pinPost(id, postId, userId);
        return ResponseEntity.ok(ApiResponse.success("Post pinned"));
    }

    /**
     * Unpin a post in the group (admin only).
     * DELETE /api/v1/communities/{id}/posts/{postId}/pin
     */
    @DeleteMapping("/{id}/posts/{postId}/pin")
    public ResponseEntity<ApiResponse<String>> unpinPost(
            @PathVariable UUID id,
            @PathVariable UUID postId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        communityService.unpinPost(id, postId, userId);
        return ResponseEntity.ok(ApiResponse.success("Post unpinned"));
    }

    // ==================== INVITES ====================

    @PostMapping("/{id}/invite")
    public ResponseEntity<ApiResponse<List<CommunityInviteResponse>>> inviteUsers(
            @PathVariable UUID id,
            @RequestBody @Valid CommunityInviteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        List<CommunityInviteResponse> invites = communityService.inviteUsers(id, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Invites sent", invites));
    }

    @GetMapping("/invites/me")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<CommunityInviteResponse>>> getMyInvites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        org.springframework.data.domain.Page<CommunityInviteResponse> invites = communityService.getMyInvites(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(invites));
    }

    @PostMapping("/invites/{inviteId}/accept")
    public ResponseEntity<ApiResponse<String>> acceptInvite(
            @PathVariable UUID inviteId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        communityService.acceptInvite(inviteId, userId);
        return ResponseEntity.ok(ApiResponse.success("Invite accepted. You joined the community."));
    }

    @PostMapping("/invites/{inviteId}/decline")
    public ResponseEntity<ApiResponse<String>> declineInvite(
            @PathVariable UUID inviteId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        communityService.declineInvite(inviteId, userId);
        return ResponseEntity.ok(ApiResponse.success("Invite declined"));
    }

    // ==================== POLLS ====================

    @PostMapping("/{id}/polls")
    public ResponseEntity<ApiResponse<CommunityPollResponse>> createPoll(
            @PathVariable UUID id,
            @RequestBody @Valid CreateCommunityPollRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        CommunityPollResponse poll = communityService.createPoll(id, userId, request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(ApiResponse.success("Poll created", poll));
    }

    @GetMapping("/{id}/polls")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<CommunityPollResponse>>> getPolls(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        org.springframework.data.domain.Page<CommunityPollResponse> polls = communityService.getCommunityPolls(id, userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(polls));
    }

    @PostMapping("/polls/{pollId}/vote")
    public ResponseEntity<ApiResponse<CommunityPollResponse>> votePoll(
            @PathVariable UUID pollId,
            @RequestParam UUID optionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        CommunityPollResponse poll = communityService.votePoll(pollId, optionId, userId);
        return ResponseEntity.ok(ApiResponse.success("Vote recorded", poll));
    }

    // ==================== EVENTS ====================

    @PostMapping("/{id}/events")
    public ResponseEntity<ApiResponse<CommunityEventResponse>> createEvent(
            @PathVariable UUID id,
            @RequestBody @Valid CreateCommunityEventRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        CommunityEventResponse event = communityService.createEvent(id, userId, request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(ApiResponse.success("Event created", event));
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<CommunityEventResponse>>> getEvents(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        org.springframework.data.domain.Page<CommunityEventResponse> events = communityService.getCommunityEvents(id, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(events));
    }
}
