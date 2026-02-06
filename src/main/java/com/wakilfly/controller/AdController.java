package com.wakilfly.controller;

import com.wakilfly.dto.request.CreateAdRequest;
import com.wakilfly.dto.response.AdResponse;
import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.model.AdType;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.AdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdController {

    private final AdService adService;
    private final CustomUserDetailsService userDetailsService;

    // ============================================
    // PUBLIC ENDPOINTS (For displaying ads)
    // ============================================

    /**
     * Get active ads to display
     * GET /api/v1/ads/active
     */
    @GetMapping("/ads/active")
    public ResponseEntity<ApiResponse<List<AdResponse>>> getActiveAds(
            @RequestParam(required = false) AdType type,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "5") int limit) {
        List<AdResponse> ads = adService.getActiveAds(type, region, limit);
        return ResponseEntity.ok(ApiResponse.success(ads));
    }

    /**
     * Record ad impression
     * POST /api/v1/ads/{adId}/impression
     */
    @PostMapping("/ads/{adId}/impression")
    public ResponseEntity<ApiResponse<Void>> recordImpression(@PathVariable UUID adId) {
        adService.recordImpression(adId);
        return ResponseEntity.ok(ApiResponse.success("Impression recorded"));
    }

    /**
     * Record ad click
     * POST /api/v1/ads/{adId}/click
     */
    @PostMapping("/ads/{adId}/click")
    public ResponseEntity<ApiResponse<Void>> recordClick(@PathVariable UUID adId) {
        adService.recordClick(adId);
        return ResponseEntity.ok(ApiResponse.success("Click recorded"));
    }

    // ============================================
    // BUSINESS OWNER ENDPOINTS (Ad management)
    // ============================================

    /**
     * Create new ad
     * POST /api/v1/business/ads
     */
    @PostMapping("/business/ads")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdResponse>> createAd(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateAdRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        AdResponse ad = adService.createAd(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Ad created successfully", ad));
    }

    /**
     * Get my ads
     * GET /api/v1/business/ads
     */
    @GetMapping("/business/ads")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<AdResponse>>> getMyAds(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<AdResponse> ads = adService.getMyAds(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(ads));
    }

    /**
     * Pause ad
     * POST /api/v1/business/ads/{adId}/pause
     */
    @PostMapping("/business/ads/{adId}/pause")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdResponse>> pauseAd(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID adId) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        AdResponse ad = adService.pauseAd(adId, userId);
        return ResponseEntity.ok(ApiResponse.success("Ad paused", ad));
    }

    /**
     * Resume ad
     * POST /api/v1/business/ads/{adId}/resume
     */
    @PostMapping("/business/ads/{adId}/resume")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdResponse>> resumeAd(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID adId) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        AdResponse ad = adService.resumeAd(adId, userId);
        return ResponseEntity.ok(ApiResponse.success("Ad resumed", ad));
    }
}
