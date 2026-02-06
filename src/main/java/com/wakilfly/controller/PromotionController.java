package com.wakilfly.controller;

import com.wakilfly.dto.request.CreatePromotionRequest;
import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.PromotionPackageResponse;
import com.wakilfly.dto.response.PromotionResponse;
import com.wakilfly.model.PromotionType;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Get promotion packages (public)
     * GET /api/v1/promotions/packages
     */
    @GetMapping("/packages")
    public ResponseEntity<ApiResponse<List<PromotionPackageResponse>>> getPackages(
            @RequestParam(required = false) PromotionType type) {
        List<PromotionPackageResponse> packages = promotionService.getPackages(type);
        return ResponseEntity.ok(ApiResponse.success(packages));
    }

    /**
     * Create a promotion
     * POST /api/v1/promotions
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PromotionResponse>> createPromotion(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreatePromotionRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PromotionResponse promotion = promotionService.createPromotion(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Promotion created. Awaiting payment.", promotion));
    }

    /**
     * Get my promotions
     * GET /api/v1/promotions
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PromotionResponse>>> getMyPromotions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<PromotionResponse> promotions = promotionService.getUserPromotions(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(promotions));
    }

    /**
     * Get promotion by ID
     * GET /api/v1/promotions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionResponse>> getPromotion(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PromotionResponse promotion = promotionService.getPromotion(id, userId);
        return ResponseEntity.ok(ApiResponse.success(promotion));
    }

    /**
     * Get promotion stats
     * GET /api/v1/promotions/{id}/stats
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<PromotionResponse.PromotionStats>> getStats(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PromotionResponse.PromotionStats stats = promotionService.getPromotionStats(id, userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Pause a promotion
     * POST /api/v1/promotions/{id}/pause
     */
    @PostMapping("/{id}/pause")
    public ResponseEntity<ApiResponse<PromotionResponse>> pausePromotion(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PromotionResponse promotion = promotionService.pausePromotion(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Promotion paused", promotion));
    }

    /**
     * Resume a promotion
     * POST /api/v1/promotions/{id}/resume
     */
    @PostMapping("/{id}/resume")
    public ResponseEntity<ApiResponse<PromotionResponse>> resumePromotion(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PromotionResponse promotion = promotionService.resumePromotion(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Promotion resumed", promotion));
    }

    /**
     * Cancel a promotion
     * DELETE /api/v1/promotions/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelPromotion(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        promotionService.cancelPromotion(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Promotion cancelled"));
    }

    /**
     * Track impression (internal/frontend call)
     * POST /api/v1/promotions/{id}/impression
     */
    @PostMapping("/{id}/impression")
    public ResponseEntity<Void> trackImpression(@PathVariable UUID id) {
        promotionService.trackImpression(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Track click (internal/frontend call)
     * POST /api/v1/promotions/{id}/click
     */
    @PostMapping("/{id}/click")
    public ResponseEntity<Void> trackClick(@PathVariable UUID id) {
        promotionService.trackClick(id);
        return ResponseEntity.ok().build();
    }
}
