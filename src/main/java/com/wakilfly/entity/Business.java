package com.wakilfly.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private User owner;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "category")
    private String category;

    @Column(name = "logo")
    private String logo;

    @Column(name = "cover_image")
    private String coverImage;

    // Location details
    @Column(name = "region")
    private String region;

    @Column(name = "district")
    private String district;

    @Column(name = "ward")
    private String ward;

    @Column(name = "street")
    private String street;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "website")
    private String website;

    // Agent who activated this business
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BusinessStatus status = BusinessStatus.PENDING;

    // Products
    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    // Subscription
    @OneToOne(mappedBy = "business", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Subscription subscription;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "trust_badge")
    private String trustBadge;

    @Column(name = "followers_count")
    @Builder.Default
    private Integer followersCount = 0;

    @Column(name = "rating")
    @Builder.Default
    private Double rating = 0.0;

    @Column(name = "reviews_count")
    @Builder.Default
    private Integer reviewsCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public int getProductsCount() {
        return products != null ? products.size() : 0;
    }
}
