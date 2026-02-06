package com.wakilfly.dto.response;

import com.wakilfly.model.CommissionStatus;
import com.wakilfly.model.CommissionType;
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
public class CommissionResponse {

    private UUID id;
    private CommissionType type;
    private BigDecimal amount;
    private CommissionStatus status;
    private String description;

    // Related business
    private UUID businessId;
    private String businessName;

    private LocalDateTime earnedAt;
    private LocalDateTime paidAt;
}
