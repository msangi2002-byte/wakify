package com.wakilfly.controller;

import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.ProductResponse;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Add product to favorites
     * POST /api/v1/favorites/{productId}
     */
    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> addToFavorites(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID productId) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        favoriteService.addToFavorites(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Added to favorites"));
    }

    /**
     * Remove product from favorites
     * DELETE /api/v1/favorites/{productId}
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeFromFavorites(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID productId) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        favoriteService.removeFromFavorites(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Removed from favorites"));
    }

    /**
     * Get my favorite products
     * GET /api/v1/favorites
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getMyFavorites(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<ProductResponse> favorites = favoriteService.getFavorites(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(favorites));
    }

    /**
     * Check if product is in favorites
     * GET /api/v1/favorites/check/{productId}
     */
    @GetMapping("/check/{productId}")
    public ResponseEntity<ApiResponse<Boolean>> checkFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID productId) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        boolean isFavorite = favoriteService.isFavorite(userId, productId);
        return ResponseEntity.ok(ApiResponse.success(isFavorite));
    }
}
