package com.wakilfly.controller;

import com.wakilfly.dto.request.UpdateBusinessRequest;
import com.wakilfly.dto.response.*;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.BusinessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;
    private final CustomUserDetailsService userDetailsService;

    // ============================================
    // PUBLIC ENDPOINTS
    // ============================================

    /**
     * Get business by ID. If authenticated, response includes isFollowing.
     * GET /api/v1/businesses/{id}
     */
    @GetMapping("/businesses/{id}")
    public ResponseEntity<ApiResponse<BusinessResponse>> getBusinessById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = null;
        if (userDetails != null) {
            currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        }
        BusinessResponse business = businessService.getBusinessById(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(business));
    }

    /**
     * Follow a business (like/follow page). Any authenticated user.
     * POST /api/v1/businesses/{id}/follow
     */
    @PostMapping("/businesses/{id}/follow")
    public ResponseEntity<ApiResponse<String>> followBusiness(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        businessService.followBusiness(userId, id);
        return ResponseEntity.ok(ApiResponse.success("You are now following this business"));
    }

    /**
     * Unfollow a business.
     * DELETE /api/v1/businesses/{id}/follow
     */
    @DeleteMapping("/businesses/{id}/follow")
    public ResponseEntity<ApiResponse<String>> unfollowBusiness(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        businessService.unfollowBusiness(userId, id);
        return ResponseEntity.ok(ApiResponse.success("You have unfollowed this business"));
    }

    /**
     * Get all active businesses
     * GET /api/v1/businesses
     */
    @GetMapping("/businesses")
    public ResponseEntity<ApiResponse<PagedResponse<BusinessResponse>>> getAllBusinesses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<BusinessResponse> businesses = businessService.getAllBusinesses(page, size);
        return ResponseEntity.ok(ApiResponse.success(businesses));
    }

    /**
     * Search businesses
     * GET /api/v1/businesses/search
     */
    @GetMapping("/businesses/search")
    public ResponseEntity<ApiResponse<PagedResponse<BusinessResponse>>> searchBusinesses(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<BusinessResponse> businesses = businessService.searchBusinesses(q, page, size);
        return ResponseEntity.ok(ApiResponse.success(businesses));
    }

    /**
     * Get businesses by category
     * GET /api/v1/businesses/category/{category}
     */
    @GetMapping("/businesses/category/{category}")
    public ResponseEntity<ApiResponse<PagedResponse<BusinessResponse>>> getBusinessesByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<BusinessResponse> businesses = businessService.getBusinessesByCategory(category, page, size);
        return ResponseEntity.ok(ApiResponse.success(businesses));
    }

    /**
     * Get businesses by region
     * GET /api/v1/businesses/region/{region}
     */
    @GetMapping("/businesses/region/{region}")
    public ResponseEntity<ApiResponse<PagedResponse<BusinessResponse>>> getBusinessesByRegion(
            @PathVariable String region,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<BusinessResponse> businesses = businessService.getBusinessesByRegion(region, page, size);
        return ResponseEntity.ok(ApiResponse.success(businesses));
    }

    // ============================================
    // BUSINESS OWNER ENDPOINTS
    // ============================================

    /**
     * Get my business profile
     * GET /api/v1/business/me
     */
    @GetMapping("/business/me")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BusinessResponse>> getMyBusiness(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        BusinessResponse business = businessService.getBusinessByOwnerId(userId);
        return ResponseEntity.ok(ApiResponse.success(business));
    }

    /**
     * Get my business dashboard (stats summary)
     * GET /api/v1/business/dashboard
     */
    @GetMapping("/business/dashboard")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BusinessDashboardResponse>> getBusinessDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        BusinessDashboardResponse dashboard = businessService.getBusinessDashboard(userId);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    /**
     * Update my business profile
     * PUT /api/v1/business/me
     */
    @PutMapping("/business/me")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BusinessResponse>> updateMyBusiness(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateBusinessRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        BusinessResponse business = businessService.updateBusiness(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Business updated successfully", business));
    }

    /**
     * Get my products
     * GET /api/v1/business/products
     */
    @GetMapping("/business/products")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getMyProducts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<ProductResponse> products = businessService.getMyProducts(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Get my orders (incoming orders)
     * GET /api/v1/business/orders
     */
    @GetMapping("/business/orders")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<OrderResponse> orders = businessService.getMyOrders(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
}
