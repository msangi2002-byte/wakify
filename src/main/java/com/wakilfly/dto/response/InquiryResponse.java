package com.wakilfly.dto.response;

import com.wakilfly.model.InquiryStatus;
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
public class InquiryResponse {

    private UUID id;
    private InquiryStatus status;

    private UUID productId;
    private String productName;
    private String productThumbnail;

    private UUID businessId;
    private String businessName;

    private UUID buyerId;
    private String buyerName;

    private String message;
    private Integer quantity;

    private String sellerReply;
    private BigDecimal quotedPrice;
    private BigDecimal quotedDeliveryFee;

    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    private UUID convertedOrderId;
}
