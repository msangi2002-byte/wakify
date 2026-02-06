package com.wakilfly.dto.response;

import com.wakilfly.model.PaymentMethod;
import com.wakilfly.model.WithdrawalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalResponse {

    private UUID id;
    private BigDecimal amount;
    private WithdrawalStatus status;
    private PaymentMethod paymentMethod;
    private String paymentPhone;
    private String paymentName;
    private String transactionId;
    private String rejectionReason;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;

    // Agent info (for admin view)
    private AgentSummary agent;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentSummary {
        private UUID id;
        private String agentCode;
        private String name;
    }
}
