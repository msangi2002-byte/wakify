package com.wakilfly.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CoinPackageResponse {
    private UUID id;
    private String name;
    private Integer coinAmount;
    private BigDecimal price;
    private Integer bonusCoins;
    private String description;
    private String iconUrl;
    private Boolean isPopular;
}
