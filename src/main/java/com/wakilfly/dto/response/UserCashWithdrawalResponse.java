package com.wakilfly.dto.response;

import com.wakilfly.model.WithdrawalStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserCashWithdrawalResponse {
    private UUID id;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentPhone;
    private String paymentName;
    private WithdrawalStatus status;
    private String transactionId;
    private String rejectionReason;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
