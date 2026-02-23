package com.wakilfly.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDraftOrderRequest {

    private BigDecimal deliveryFee;
    private BigDecimal discount;
    private String sellerNotes;
}
