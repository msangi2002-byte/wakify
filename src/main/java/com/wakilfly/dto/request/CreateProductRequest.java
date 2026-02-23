package com.wakilfly.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    private BigDecimal compareAtPrice; // Original price for discounts

    private String category;

    private Integer stockQuantity;

    /** Minimum order quantity (MOQ) for B2B. Default 1. */
    private Integer minOrderQuantity;

    private Boolean trackStock;

    /** Feature on marketplace "Picked for you" section */
    private Boolean isFeatured;

    private List<String> tags;
}
