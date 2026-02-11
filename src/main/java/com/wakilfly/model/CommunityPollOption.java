package com.wakilfly.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "community_poll_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityPollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private CommunityPoll poll;

    @Column(nullable = false, length = 300)
    private String text;

    @Column(name = "display_order")
    private int displayOrder;

    @Column(name = "votes_count")
    @Builder.Default
    private Integer votesCount = 0;
}
