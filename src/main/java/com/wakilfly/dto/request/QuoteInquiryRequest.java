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
public class QuoteInquiryRequest {

    private String sellerReply;

    private BigDecimal quotedPrice;

    private BigDecimal quotedDeliveryFee;
}
