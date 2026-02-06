package com.wakilfly.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AddToCartRequest {
    @NotNull
    private UUID productId;

    @Min(1)
    private Integer quantity = 1;
}
