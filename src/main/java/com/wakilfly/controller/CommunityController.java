package com.wakilfly.controller;

import com.wakilfly.dto.request.CreateCommunityRequest;
import com.wakilfly.dto.response.ApiResponse;
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
}
