package com.wakilfly.controller;

import com.wakilfly.dto.request.CreateSubscriptionRequest;
import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.SubscriptionPlansResponse;
import com.wakilfly.dto.response.SubscriptionResponse;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CustomUserDetailsService userDetailsService;

    // ============================================
    // PUBLIC ENDPOINTS
    // ============================================

    /**
     * Get available subscription plans
     * GET /api/v1/subscriptions/plans
     */
    @GetMapping("/subscriptions/plans")
    public ResponseEntity<ApiResponse<SubscriptionPlansResponse>> getPlans() {
        SubscriptionPlansResponse plans = subscriptionService.getPlans();
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    // ============================================
    // BUSINESS OWNER ENDPOINTS
    // ============================================

    /**
     * Create new subscription (initiate payment)
     * POST /api/v1/business/subscription
     */
    @PostMapping("/business/subscription")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateSubscriptionRequest request) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        SubscriptionResponse subscription = subscriptionService.createSubscription(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subscription created. Please complete payment.", subscription));
    }

    /**
     * Get my subscription
     * GET /api/v1/business/subscription
     */
    @GetMapping("/business/subscription")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getMySubscription(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        SubscriptionResponse subscription = subscriptionService.getSubscription(userId);
        return ResponseEntity.ok(ApiResponse.success(subscription));
    }

    /**
     * Cancel subscription
     * POST /api/v1/business/subscription/cancel
     */
    @PostMapping("/business/subscription/cancel")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancelSubscription(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        SubscriptionResponse subscription = subscriptionService.cancelSubscription(userId);
        return ResponseEntity.ok(ApiResponse.success("Subscription cancelled", subscription));
    }

    /**
     * Toggle auto-renew
     * PUT /api/v1/business/subscription/auto-renew
     */
    @PutMapping("/business/subscription/auto-renew")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> toggleAutoRenew(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Boolean> body) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        boolean autoRenew = body.getOrDefault("autoRenew", false);
        SubscriptionResponse subscription = subscriptionService.toggleAutoRenew(userId, autoRenew);
        return ResponseEntity.ok(ApiResponse.success(
                autoRenew ? "Auto-renew enabled" : "Auto-renew disabled", subscription));
    }

    /**
     * Renew subscription (create new payment for renewal)
     * POST /api/v1/business/subscription/renew
     */
    @PostMapping("/business/subscription/renew")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> renewSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateSubscriptionRequest request) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        SubscriptionResponse subscription = subscriptionService.createSubscription(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Renewal initiated. Please complete payment.", subscription));
    }
}
