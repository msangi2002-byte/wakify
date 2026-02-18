package com.wakilfly.dto.request;

import com.wakilfly.model.PromotionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simplified request for Product/Business boost – creates promotion and initiates USSD payment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoostProductOrBusinessRequest {

    @NotNull
    private PromotionType type; // PRODUCT or BUSINESS

    @NotNull
    private UUID targetId; // Product ID or Business ID

    @NotNull
    @Min(5000)
    private BigDecimal budget;

    @NotNull
    private String paymentPhone;
}
