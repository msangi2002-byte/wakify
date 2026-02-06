package com.wakilfly.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "virtual_gifts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualGift {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name; // e.g. "Rose", "Heart", "Crown", "Diamond"

    @Column(length = 500)
    private String description;

    @Column(name = "icon_url")
    private String iconUrl; // Gift icon/animation URL

    @Column(name = "animation_url")
    private String animationUrl; // Lottie/GIF animation

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // Price in coins/TZS

    @Column(name = "coin_value")
    private Integer coinValue; // Value in app coins

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_premium")
    @Builder.Default
    private Boolean isPremium = false; // For special gifts

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
