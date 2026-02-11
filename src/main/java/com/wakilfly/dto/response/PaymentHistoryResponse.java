package com.wakilfly.dto.response;

import com.wakilfly.model.PaymentMethod;
import com.wakilfly.model.PaymentStatus;
import com.wakilfly.model.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment history item for user profile (malipo yote – coins, subscription, n.k.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryResponse {
    private UUID id;
    private String transactionId;
    private BigDecimal amount;
    private PaymentType type;
    private PaymentStatus status;
    private PaymentMethod method;
    private String paymentPhone;
    private String description;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
