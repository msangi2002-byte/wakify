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
    /** Creator earnings from gifts (diamonds). Convert to TZS at diamondToTzsRate for withdrawal. */
    private BigDecimal diamondBalance;
    private Integer totalCoinsPurchased;
    private Integer totalCoinsSpent;
    private BigDecimal totalGiftsReceived;
    /** 1 diamond = X TZS (for display and withdrawal). */
    private BigDecimal diamondToTzsRate;
    /** Display: 1 coin ≈ X TZS when buying coins (in-app). */
    private BigDecimal coinToTzsBuyRate;
    /** Withdrawable amount in TZS (diamondBalance * rate). */
    private BigDecimal withdrawableTzs;
    private BigDecimal minWithdrawalTzs;
    private BigDecimal maxWithdrawalTzs;
}
