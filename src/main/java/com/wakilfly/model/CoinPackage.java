package com.wakilfly.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "coin_packages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name; // e.g. "Starter Pack", "Value Pack"

    @Column(name = "coin_amount", nullable = false)
    private Integer coinAmount; // Number of coins

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // Price in TZS

    @Column(name = "bonus_coins")
    @Builder.Default
    private Integer bonusCoins = 0; // Extra coins as bonus

    @Column(length = 255)
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "is_popular")
    @Builder.Default
    private Boolean isPopular = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}
