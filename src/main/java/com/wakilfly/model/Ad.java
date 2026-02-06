package com.wakilfly.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "target_url")
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AdStatus status = AdStatus.PENDING;

    // Budget and Pricing
    @Column(name = "daily_budget", precision = 15, scale = 2)
    private BigDecimal dailyBudget;

    @Column(name = "total_budget", precision = 15, scale = 2)
    private BigDecimal totalBudget;

    @Column(name = "cost_per_click", precision = 10, scale = 2)
    private BigDecimal costPerClick;

    @Column(name = "cost_per_view", precision = 10, scale = 2)
    private BigDecimal costPerView;

    // Targeting
    @Column(name = "target_regions")
    private String targetRegions; // Comma-separated regions

    @Column(name = "target_categories")
    private String targetCategories; // Target product categories

    // Schedule
    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    // Stats
    @Column(name = "impressions")
    @Builder.Default
    private Integer impressions = 0;

    @Column(name = "clicks")
    @Builder.Default
    private Integer clicks = 0;

    @Column(name = "amount_spent", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal amountSpent = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
