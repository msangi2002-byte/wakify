package com.wakilfly.controller;

import com.wakilfly.dto.request.ReviewRequest;
import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.ReviewResponse;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> addReview(
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        ReviewResponse response = reviewService.addReview(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getProductReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponse<ReviewResponse> response = reviewService.getProductReviews(productId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
