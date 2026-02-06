package com.wakilfly.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class VirtualGiftResponse {
    private UUID id;
    private String name;
    private String description;
    private String iconUrl;
    private String animationUrl;
    private BigDecimal price;
    private Integer coinValue;
    private Boolean isPremium;
}
