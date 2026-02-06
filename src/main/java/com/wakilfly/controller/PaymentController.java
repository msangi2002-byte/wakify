package com.wakilfly.controller;

import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.model.Payment;
import com.wakilfly.model.PaymentType;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Initiate a payment via HarakaPay
     */
    @PostMapping("/initiate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentInitiateResponse>> initiatePayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PaymentInitiateRequest request) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();

        String orderId = paymentService.initiatePayment(
                userId,
                request.getAmount(),
                request.getType(),
                request.getPhone(),
                request.getDescription());

        PaymentInitiateResponse response = new PaymentInitiateResponse();
        response.setOrderId(orderId);
        response.setMessage("USSD push imetumwa kwa simu yako. Fuata maelekezo kukamilisha malipo.");
        response.setAmount(request.getAmount());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ombi la malipo limetumwa", response));
    }

    /**
     * Check payment status
     */
    @GetMapping("/status/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> checkPaymentStatus(
            @PathVariable String orderId) {

        Payment payment = paymentService.refreshPaymentStatus(orderId);

        if (payment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Malipo hayakupatikana"));
        }

        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setOrderId(orderId);
        response.setStatus(payment.getStatus().name());
        response.setAmount(payment.getAmount());
        response.setType(payment.getType().name());
        response.setPaidAt(payment.getPaidAt() != null ? payment.getPaidAt().toString() : null);
        response.setCreatedAt(payment.getCreatedAt().toString());

        String message = switch (payment.getStatus()) {
            case PENDING -> "Malipo yanasubiri kukamilishwa";
            case PROCESSING -> "Malipo yanashughulikiwa";
            case SUCCESS -> "Malipo yamekamilika!";
            case FAILED -> "Malipo yameshindikana";
            case CANCELLED -> "Malipo yameghairiwa";
            case REFUNDED -> "Malipo yamerudishwa";
        };

        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    /**
     * Get wallet balance (Admin only)
     */
    @GetMapping("/balance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWalletBalance() {
        Map<String, Object> balance = paymentService.getWalletBalance();

        if (Boolean.TRUE.equals(balance.get("success"))) {
            return ResponseEntity.ok(ApiResponse.success("Salio limepatikana", balance));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Imeshindikana kupata salio"));
        }
    }

    /**
     * Get payment by ID
     */
    @GetMapping("/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID paymentId) {

        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        Payment payment = paymentService.getPaymentById(paymentId);

        if (payment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Malipo hayakupatikana"));
        }

        // Check ownership
        if (!payment.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Hauruhusiwi kuona malipo haya"));
        }

        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setOrderId(payment.getTransactionId());
        response.setStatus(payment.getStatus().name());
        response.setAmount(payment.getAmount());
        response.setType(payment.getType().name());
        response.setPaidAt(payment.getPaidAt() != null ? payment.getPaidAt().toString() : null);
        response.setCreatedAt(payment.getCreatedAt().toString());

        return ResponseEntity.ok(ApiResponse.success("Malipo yamepatikana", response));
    }

    // Request/Response DTOs

    @Data
    public static class PaymentInitiateRequest {
        @NotNull(message = "Kiasi kinahitajika")
        @Min(value = 100, message = "Kiasi cha chini ni TZS 100")
        private BigDecimal amount;

        @NotNull(message = "Aina ya malipo inahitajika")
        private PaymentType type;

        @NotBlank(message = "Namba ya simu inahitajika")
        private String phone;

        private String description;
    }

    @Data
    public static class PaymentInitiateResponse {
        private String orderId;
        private String message;
        private BigDecimal amount;
    }

    @Data
    public static class PaymentStatusResponse {
        private String orderId;
        private String status;
        private BigDecimal amount;
        private String type;
        private String paidAt;
        private String createdAt;
    }
}
