package com.wakilfly.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAgentPackageRequest {

    @NotBlank(message = "Package name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be positive")
    private BigDecimal price;

    @NotNull(message = "Number of businesses is required")
    @Min(value = 1, message = "Number of businesses must be at least 1")
    private Integer numberOfBusinesses;

    private Boolean isActive = true;

    private Boolean isPopular = false;

    private Integer sortOrder = 0;
}
