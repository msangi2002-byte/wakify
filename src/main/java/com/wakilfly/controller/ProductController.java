package com.wakilfly.controller;

import com.wakilfly.dto.request.CreateProductRequest;
import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.ProductResponse;
import com.wakilfly.model.Business;
import com.wakilfly.repository.BusinessRepository;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final BusinessRepository businessRepository;
    private final CustomUserDetailsService userDetailsService;

    // ============================================
    // PUBLIC ENDPOINTS (Marketplace)
    // ============================================

    /**
     * Get all products (marketplace)
     * GET /api/v1/products
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ProductResponse> products = productService.getAllProducts(page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Get product by ID
     * GET /api/v1/products/{id}
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable UUID id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    /**
     * Search products
     * GET /api/v1/products/search
     */
    @GetMapping("/products/search")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> searchProducts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ProductResponse> products = productService.searchProducts(q, page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Get products by category
     * GET /api/v1/products/category/{category}
     */
    @GetMapping("/products/category/{category}")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getProductsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ProductResponse> products = productService.getProductsByCategory(category, page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Get trending products (by views)
     * GET /api/v1/products/trending
     */
    @GetMapping("/products/trending")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getTrendingProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ProductResponse> products = productService.getTrendingProducts(page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Get top selling products (by orders count – marketplace hero)
     * GET /api/v1/products/top-selling
     */
    @GetMapping("/products/top-selling")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getTopSellingProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ProductResponse> products = productService.getTopSellingProducts(page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Get featured products (picked for you)
     * GET /api/v1/products/featured
     */
    @GetMapping("/products/featured")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getFeaturedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ProductResponse> products = productService.getFeaturedProducts(page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Get products by region
     * GET /api/v1/products/region/{region}
     */
    @GetMapping("/products/region/{region}")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getProductsByRegion(
            @PathVariable String region,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ProductResponse> products = productService.getProductsByRegion(region, page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Get products by business
     * GET /api/v1/products/business/{businessId}
     */
    @GetMapping("/products/business/{businessId}")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getBusinessProducts(
            @PathVariable UUID businessId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ProductResponse> products = productService.getBusinessProducts(businessId, page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    // ============================================
    // BUSINESS OWNER ENDPOINTS
    // ============================================

    /**
     * Create a new product
     * POST /api/v1/business/products
     */
    @PostMapping(value = "/business/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("product") @Valid CreateProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        Business business = businessRepository.findByOwnerId(userId)
                .orElseThrow(() -> new RuntimeException("Business not found for this user"));

        ProductResponse product = productService.createProduct(business.getId(), request, images);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", product));
    }

    /**
     * Update a product (JSON only)
     * PUT /api/v1/business/products/{id}
     */
    @PutMapping(value = "/business/products/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody CreateProductRequest request) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        Business business = businessRepository.findByOwnerId(userId)
                .orElseThrow(() -> new RuntimeException("Business not found for this user"));

        ProductResponse product = productService.updateProduct(id, business.getId(), request, null, null);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", product));
    }

    /**
     * Update a product with optional cover image and gallery images (multipart)
     * PUT /api/v1/business/products/{id}
     */
    @PutMapping(value = "/business/products/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductWithImages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @RequestPart("product") @Valid CreateProductRequest request,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        Business business = businessRepository.findByOwnerId(userId)
                .orElseThrow(() -> new RuntimeException("Business not found for this user"));

        ProductResponse product = productService.updateProduct(id, business.getId(), request, coverImage, images);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", product));
    }

    /**
     * Delete a product
     * DELETE /api/v1/business/products/{id}
     */
    @DeleteMapping("/business/products/{id}")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        Business business = businessRepository.findByOwnerId(userId)
                .orElseThrow(() -> new RuntimeException("Business not found for this user"));

        productService.deleteProduct(id, business.getId());
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }

    /**
     * Get my products (business owner)
     * GET /api/v1/products/my
     */
    @GetMapping("/products/my")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getMyProducts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        Business business = businessRepository.findByOwnerId(userId)
                .orElseThrow(() -> new RuntimeException("Business not found for this user"));

        PagedResponse<ProductResponse> products = productService.getBusinessProducts(business.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }
}
