package com.wakilfly.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records each time a user watches a reel: watch time and whether they completed it.
 * Used for Reels algorithm: watch time (0.4) + completion rate (0.3) + shares + comments + likes.
 */
@Entity
@Table(name = "reel_views", indexes = {
        @Index(name = "idx_reel_views_post_id", columnList = "post_id"),
        @Index(name = "idx_reel_views_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReelView {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** How many seconds the user watched before leaving/skipping. */
    @Column(name = "watch_time_seconds", nullable = false)
    private int watchTimeSeconds;

    /** True if user watched most of the reel (e.g. ≥90% of duration). */
    @Column(name = "completed", nullable = false)
    private boolean completed;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
