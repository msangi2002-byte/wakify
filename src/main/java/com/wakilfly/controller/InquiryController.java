package com.wakilfly.controller;

import com.wakilfly.dto.request.CreateInquiryRequest;
import com.wakilfly.dto.request.QuoteInquiryRequest;
import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.InquiryResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.model.Business;
import com.wakilfly.model.InquiryStatus;
import com.wakilfly.repository.BusinessRepository;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;
    private final BusinessRepository businessRepository;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Create inquiry (Contact Supplier / Request for Quotation)
     * POST /api/v1/inquiries
     */
    @PostMapping("/inquiries")
    public ResponseEntity<ApiResponse<InquiryResponse>> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateInquiryRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        InquiryResponse inquiry = inquiryService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inquiry sent", inquiry));
    }

    /**
     * Get my inquiries (buyer)
     * GET /api/v1/inquiries/my
     */
    @GetMapping("/inquiries/my")
    public ResponseEntity<ApiResponse<PagedResponse<InquiryResponse>>> getMyInquiries(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<InquiryResponse> list = inquiryService.getMyInquiries(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * Get inquiry by ID
     * GET /api/v1/inquiries/{id}
     */
    @GetMapping("/inquiries/{id}")
    public ResponseEntity<ApiResponse<InquiryResponse>> getById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        InquiryResponse inquiry = inquiryService.getById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(inquiry));
    }

    /**
     * Seller sends quote
     * POST /api/v1/business/inquiries/{id}/quote
     */
    @PostMapping("/business/inquiries/{id}/quote")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InquiryResponse>> quote(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody QuoteInquiryRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        InquiryResponse inquiry = inquiryService.quote(id, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Quote sent", inquiry));
    }

    /**
     * Buyer accepts quote
     * POST /api/v1/inquiries/{id}/accept
     */
    @PostMapping("/inquiries/{id}/accept")
    public ResponseEntity<ApiResponse<InquiryResponse>> accept(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        InquiryResponse inquiry = inquiryService.accept(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Quote accepted", inquiry));
    }

    /**
     * Reject inquiry (buyer or seller)
     * POST /api/v1/inquiries/{id}/reject
     */
    @PostMapping("/inquiries/{id}/reject")
    public ResponseEntity<ApiResponse<InquiryResponse>> reject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean asSeller) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        InquiryResponse inquiry = inquiryService.reject(id, userId, asSeller);
        return ResponseEntity.ok(ApiResponse.success("Inquiry closed", inquiry));
    }

    /**
     * Get inquiries for my business (seller)
     * GET /api/v1/business/inquiries
     */
    @GetMapping("/business/inquiries")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<InquiryResponse>>> getBusinessInquiries(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        Business business = businessRepository.findByOwnerId(userId)
                .orElseThrow(() -> new RuntimeException("Business not found"));
        PagedResponse<InquiryResponse> list = inquiryService.getBusinessInquiries(business.getId(), userId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
