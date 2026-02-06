package com.wakilfly.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "live_streams")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveStream {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LiveStreamStatus status = LiveStreamStatus.SCHEDULED;

    @Column(name = "room_id", unique = true)
    private String roomId; // Unique room for streaming

    @Column(name = "stream_key")
    private String streamKey; // For RTMP streaming

    // Stats
    @Column(name = "viewer_count")
    @Builder.Default
    private Integer viewerCount = 0;

    @Column(name = "peak_viewers")
    @Builder.Default
    private Integer peakViewers = 0;

    @Column(name = "total_gifts_value", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalGiftsValue = BigDecimal.ZERO;

    @Column(name = "likes_count")
    @Builder.Default
    private Integer likesCount = 0;

    @Column(name = "comments_count")
    @Builder.Default
    private Integer commentsCount = 0;

    // Schedule
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    // Gifts received
    @OneToMany(mappedBy = "liveStream", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GiftTransaction> giftTransactions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public void incrementViewers() {
        this.viewerCount = (this.viewerCount == null ? 0 : this.viewerCount) + 1;
        if (this.viewerCount > (this.peakViewers == null ? 0 : this.peakViewers)) {
            this.peakViewers = this.viewerCount;
        }
    }

    public void decrementViewers() {
        this.viewerCount = Math.max(0, (this.viewerCount == null ? 0 : this.viewerCount) - 1);
    }

    public void addGiftValue(BigDecimal value) {
        this.totalGiftsValue = this.totalGiftsValue.add(value);
    }
}
