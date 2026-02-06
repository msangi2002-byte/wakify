package com.wakilfly.controller;

import com.wakilfly.dto.request.AddToCartRequest;
import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.CartResponse;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CustomUserDetailsService userDetailsService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        CartResponse response = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        CartResponse response = cartService.addToCart(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @PathVariable UUID productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        CartResponse response = cartService.removeFromCart(userId, productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<String>> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared"));
    }
}
