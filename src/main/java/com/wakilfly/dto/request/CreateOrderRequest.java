package com.wakilfly.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotEmpty(message = "Order items cannot be empty")
    private List<OrderItemRequest> items;

    // Delivery details
    @NotNull(message = "Delivery name is required")
    private String deliveryName;

    @NotNull(message = "Delivery phone is required")
    private String deliveryPhone;

    @NotNull(message = "Delivery address is required")
    private String deliveryAddress;

    private String deliveryRegion;
    private String deliveryDistrict;

    private String customerNotes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        @NotNull(message = "Product ID is required")
        private UUID productId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}
