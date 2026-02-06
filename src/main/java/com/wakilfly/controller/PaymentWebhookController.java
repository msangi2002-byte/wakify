package com.wakilfly.controller;

import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.model.Payment;
import com.wakilfly.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final PaymentService paymentService;

    /**
     * HarakaPay payment callback
     * POST /api/v1/webhooks/harakapay
     */
    @PostMapping("/harakapay")
    public ResponseEntity<ApiResponse<Void>> harakaPayCallback(@RequestBody Map<String, Object> payload) {
        log.info("HarakaPay callback received: {}", payload);

        // Extract order_id and process
        String orderId = (String) payload.get("order_id");
        if (orderId != null) {
            paymentService.refreshPaymentStatus(orderId);
        }

        return ResponseEntity.ok(ApiResponse.success("Callback received"));
    }

    /**
     * Check payment status (public for mobile apps)
     * GET /api/v1/webhooks/payment/status/{orderId}
     */
    @GetMapping("/payment/status/{orderId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkPaymentStatus(
            @PathVariable String orderId) {
        Payment payment = paymentService.getPaymentByOrderId(orderId);

        if (payment == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> status = Map.of(
                "orderId", payment.getTransactionId() != null ? payment.getTransactionId() : "",
                "status", payment.getStatus().name(),
                "amount", payment.getAmount(),
                "type", payment.getType().name(),
                "paidAt", payment.getPaidAt() != null ? payment.getPaidAt().toString() : "",
                "createdAt", payment.getCreatedAt().toString());

        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Refresh payment status manually
     * POST /api/v1/webhooks/payment/refresh/{orderId}
     */
    @PostMapping("/payment/refresh/{orderId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshPaymentStatus(
            @PathVariable String orderId) {
        Payment payment = paymentService.refreshPaymentStatus(orderId);

        if (payment == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> status = Map.of(
                "orderId", payment.getTransactionId() != null ? payment.getTransactionId() : "",
                "status", payment.getStatus().name(),
                "amount", payment.getAmount(),
                "type", payment.getType().name(),
                "paidAt", payment.getPaidAt() != null ? payment.getPaidAt().toString() : "");

        String message = switch (payment.getStatus()) {
            case PENDING -> "Malipo yanasubiri kukamilishwa";
            case PROCESSING -> "Malipo yanashughulikiwa";
            case SUCCESS -> "Malipo yamekamilika!";
            case FAILED -> "Malipo yameshindikana";
            case CANCELLED -> "Malipo yameghairiwa";
            case REFUNDED -> "Malipo yamerudishwa";
        };

        return ResponseEntity.ok(ApiResponse.success(message, status));
    }
}
