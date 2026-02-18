package com.wakilfly.controller;

import com.wakilfly.dto.request.BoostPostRequest;
import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.PostResponse;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.PostBoostService;
import com.wakilfly.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.wakilfly.model.PromotionObjective;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ads")
@RequiredArgsConstructor
public class PostBoostController {

    private final PostBoostService postBoostService;
    private final PostService postService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Get my posts for boosting
     * GET /api/v1/ads/posts/my
     */
    @GetMapping("/posts/my")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getMyPosts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<PostResponse> posts = postService.getUserPosts(userId, page, size, userId);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    /**
     * Calculate ad price based on target reach
     * GET /api/v1/ads/calculate-price?targetReach=1000
     */
    @GetMapping("/calculate-price")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculatePrice(
            @RequestParam long targetReach) {
        BigDecimal pricePerPerson = postBoostService.getAdsPricePerPerson();
        BigDecimal totalPrice = pricePerPerson.multiply(BigDecimal.valueOf(targetReach));
        
        Map<String, Object> response = Map.of(
            "targetReach", targetReach,
            "pricePerPerson", pricePerPerson,
            "totalPrice", totalPrice
        );
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Boost a post - create promotion and initiate USSD payment.
     * Optional: objective (ENGAGEMENT, MESSAGES, TRAFFIC), audienceType (AUTOMATIC, LOCAL), targeting.
     * POST /api/v1/ads/boost-post
     */
    @PostMapping("/boost-post")
    public ResponseEntity<ApiResponse<Map<String, Object>>> boostPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody BoostPostRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        UUID postId = UUID.fromString(request.getPostId());
        long targetReach = request.getTargetReach() > 0 ? request.getTargetReach() : 1000;
        String paymentPhone = request.getPaymentPhone();
        PromotionObjective objective = parseObjective(request.getObjective());
        String audienceType = request.getAudienceType() != null ? request.getAudienceType().toUpperCase() : "AUTOMATIC";
        String targetRegionsCsv = null;
        if (request.getTargetRegions() != null && !request.getTargetRegions().isEmpty()) {
            targetRegionsCsv = String.join(",", request.getTargetRegions());
        }
        Map<String, Object> result = postBoostService.createPostBoost(
                userId, postId, targetReach, paymentPhone,
                objective, audienceType, targetRegionsCsv,
                request.getTargetAgeMin(), request.getTargetAgeMax(), request.getTargetGender());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post boost created. Payment initiated.", result));
    }

    private static PromotionObjective parseObjective(String s) {
        if (s == null || s.isBlank()) return PromotionObjective.ENGAGEMENT;
        try {
            return PromotionObjective.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PromotionObjective.ENGAGEMENT;
        }
    }

    /**
     * Get my boost analytics
     * GET /api/v1/ads/analytics
     */
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBoostAnalytics(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        Map<String, Object> analytics = postBoostService.getBoostAnalytics(userId);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
}
