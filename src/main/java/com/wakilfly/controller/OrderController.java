package com.wakilfly.controller;

import com.wakilfly.dto.request.AddTrackingEventRequest;
import com.wakilfly.dto.request.CreateDraftOrderRequest;
import com.wakilfly.dto.request.CreateOrderRequest;
import com.wakilfly.dto.request.UpdateDraftOrderRequest;
import com.wakilfly.dto.request.UpdateOrderStatusRequest;
import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.OrderResponse;
import com.wakilfly.dto.response.OrderTrackingEventResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.model.Business;
import com.wakilfly.model.OrderStatus;
import com.wakilfly.repository.BusinessRepository;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.OrderService;
import com.wakilfly.service.OrderTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderTrackingService orderTrackingService;
    private final BusinessRepository businessRepository;
    private final CustomUserDetailsService userDetailsService;

    // ============================================
    // BUYER ENDPOINTS
    // ============================================

    /**
     * Create draft order from accepted inquiry (Alibaba-style)
     * POST /api/v1/orders/draft
     */
    @PostMapping("/orders/draft")
    public ResponseEntity<ApiResponse<OrderResponse>> createDraftOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateDraftOrderRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        OrderResponse order = orderService.createDraftOrderFromInquiry(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Draft order created", order));
    }

    /**
     * Buyer confirms draft order (ready for payment)
     * POST /api/v1/orders/{id}/confirm
     */
    @PostMapping("/orders/{id}/confirm")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrderByBuyer(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        OrderResponse order = orderService.confirmOrderByBuyer(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Order confirmed", order));
    }

    /**
     * Create a new order (buyer) – cart checkout
     * POST /api/v1/orders
     */
    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateOrderRequest request) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        OrderResponse order = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", order));
    }

    /**
     * Get my orders (buyer)
     * GET /api/v1/orders/my
     */
    @GetMapping("/orders/my")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<OrderResponse> orders = orderService.getBuyerOrders(userId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * Get order by ID
     * GET /api/v1/orders/{id}
     */
    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        OrderResponse order = orderService.getOrderById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * Cancel my order (buyer)
     * POST /api/v1/orders/{id}/cancel
     */
    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        String reason = body != null ? body.get("reason") : null;
        OrderResponse order = orderService.cancelOrder(id, userId, reason);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", order));
    }

    // ============================================
    // SELLER (BUSINESS) ENDPOINTS
    // ============================================

    /**
     * Get business orders (seller) - with status filter
     * GET /api/v1/orders/incoming
     */
    @GetMapping("/orders/incoming")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getBusinessOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        Business business = businessRepository.findByOwnerId(userId)
                .orElseThrow(() -> new RuntimeException("Business not found for this user"));

        PagedResponse<OrderResponse> orders = orderService.getBusinessOrders(business.getId(), status, page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * Seller updates draft order (delivery fee, discount, notes)
     * PUT /api/v1/business/orders/{id}/draft
     */
    @PutMapping("/business/orders/{id}/draft")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateDraftOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDraftOrderRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        OrderResponse order = orderService.updateDraftOrder(id, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Draft updated", order));
    }

    /**
     * Update order status (seller)
     * PUT /api/v1/business/orders/{id}/status
     */
    @PutMapping("/business/orders/{id}/status")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        OrderResponse order = orderService.updateOrderStatus(id, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", order));
    }

    /**
     * Confirm order (seller shortcut)
     * POST /api/v1/business/orders/{id}/confirm
     */
    @PostMapping("/business/orders/{id}/confirm")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();
        OrderResponse order = orderService.updateOrderStatus(id, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Order confirmed", order));
    }

    /**
     * Mark order as shipped (seller)
     * POST /api/v1/business/orders/{id}/ship
     */
    @PostMapping("/business/orders/{id}/ship")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> shipOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.SHIPPED)
                .trackingNumber(body != null ? body.get("trackingNumber") : null)
                .build();
        OrderResponse order = orderService.updateOrderStatus(id, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Order marked as shipped", order));
    }

    /**
     * Mark order as delivered (seller)
     * POST /api/v1/business/orders/{id}/deliver
     */
    @PostMapping("/business/orders/{id}/deliver")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> deliverOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.DELIVERED)
                .build();
        OrderResponse order = orderService.updateOrderStatus(id, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Order marked as delivered", order));
    }

    /**
     * Add tracking event (seller) – e.g. AT_STORE, PACKAGING, SHIPPED, IN_TRANSIT, DELIVERED with optional location
     * POST /api/v1/business/orders/{id}/tracking
     */
    @PostMapping("/business/orders/{id}/tracking")
    @PreAuthorize("hasRole('BUSINESS') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderTrackingEventResponse>> addTrackingEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody AddTrackingEventRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        OrderTrackingEventResponse event = orderTrackingService.addEvent(id, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tracking updated", event));
    }

    /**
     * Get tracking events for an order (buyer or seller)
     * GET /api/v1/orders/{id}/tracking
     */
    @GetMapping("/orders/{id}/tracking")
    public ResponseEntity<ApiResponse<List<OrderTrackingEventResponse>>> getOrderTracking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        List<OrderTrackingEventResponse> events = orderTrackingService.getEventsForOrder(id, userId);
        return ResponseEntity.ok(ApiResponse.success(events));
    }
}
