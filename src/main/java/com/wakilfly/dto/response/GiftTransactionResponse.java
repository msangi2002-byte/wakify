package com.wakilfly.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class GiftTransactionResponse {
    private UUID id;
    private String senderName;
    private String senderPic;
    private String receiverName;
    private String receiverPic;
    private String giftName;
    private String giftIcon;
    private Integer quantity;
    private BigDecimal totalValue;
    private String message;
    private LocalDateTime createdAt;
}
