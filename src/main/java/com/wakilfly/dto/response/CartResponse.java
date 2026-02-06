package com.wakilfly.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CartResponse {
    private UUID id;
    private List<CartItemResponse> items;
    private BigDecimal summaryTotal;
    private Integer itemCount;

    @Data
    @Builder
    public static class CartItemResponse {
        private UUID id;
        private UUID productId;
        private String productName;
        private String productImage;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal total;
    }
}
