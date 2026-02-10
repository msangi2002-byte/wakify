package com.wakilfly.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Viewer requests to join a live stream as guest (e.g. TikTok/Instagram style).
 * Host can accept or reject.
 */
@Entity
@Table(name = "live_stream_join_requests", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "live_stream_id", "requester_id" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveStreamJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_stream_id", nullable = false)
    private LiveStream liveStream;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JoinRequestStatus status = JoinRequestStatus.PENDING;

    @Column(name = "host_responded_at")
    private LocalDateTime hostRespondedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
