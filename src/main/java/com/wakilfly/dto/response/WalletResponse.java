package com.wakilfly.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class WalletResponse {
    private UUID id;
    private Integer coinBalance;
    private BigDecimal cashBalance;
    private Integer totalCoinsPurchased;
    private Integer totalCoinsSpent;
    private BigDecimal totalGiftsReceived;
}
