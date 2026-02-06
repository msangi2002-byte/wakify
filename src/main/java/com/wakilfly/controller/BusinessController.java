package com.wakilfly.controller;

import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.BusinessResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.BusinessService;
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
     * Get business by ID
     * GET /api/v1/businesses/{id}
     */
    @GetMapping("/businesses/{id}")
    public ResponseEntity<ApiResponse<BusinessResponse>> getBusinessById(@PathVariable UUID id) {
        BusinessResponse business = businessService.getBusinessById(id);
        return ResponseEntity.ok(ApiResponse.success(business));
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
}
