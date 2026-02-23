package com.wakilfly.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInquiryRequest {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotBlank(message = "Message is required")
    private String message;

    private Integer quantity;
}
