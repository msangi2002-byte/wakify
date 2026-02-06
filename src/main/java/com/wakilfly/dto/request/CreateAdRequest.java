package com.wakilfly.dto.request;

import com.wakilfly.model.AdType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateAdRequest {
    @NotBlank
    private String title;

    private String description;
    private String imageUrl;
    private String videoUrl;
    private String targetUrl;

    @NotNull
    private AdType type;

    private BigDecimal dailyBudget;
    private BigDecimal totalBudget;

    private String targetRegions;
    private String targetCategories;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
