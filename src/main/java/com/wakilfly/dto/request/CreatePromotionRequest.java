package com.wakilfly.dto.request;

import com.wakilfly.model.PromotionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePromotionRequest {

    @NotNull(message = "Promotion type is required")
    private PromotionType type;

    @NotNull(message = "Target ID is required")
    private UUID targetId; // Post, Product, or Business ID

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Budget is required")
    @Min(value = 5000, message = "Minimum budget is 5000 TZS")
    private BigDecimal budget;

    private BigDecimal dailyBudget;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    // Targeting options
    private List<String> targetRegions;
    private Integer targetAgeMin;
    private Integer targetAgeMax;
    private String targetGender; // ALL, MALE, FEMALE

    // Payment
    private String paymentPhone;
}
