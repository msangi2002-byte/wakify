package com.wakilfly.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gift_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiftTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gift_id", nullable = false)
    private VirtualGift gift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_stream_id")
    private LiveStream liveStream; // Null if sent outside live

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "total_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalValue;

    @Column(length = 255)
    private String message; // Optional message with gift

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
