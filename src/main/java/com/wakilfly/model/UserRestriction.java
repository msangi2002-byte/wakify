package com.wakilfly.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Restricted list: restricter only shows public content to restricted user.
 * Unlike block, they remain "friends"/followers but see limited profile.
 */
@Entity
@Table(name = "user_restrictions", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "restricter_id", "restricted_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restricter_id", nullable = false)
    private User restricter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restricted_id", nullable = false)
    private User restricted;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
