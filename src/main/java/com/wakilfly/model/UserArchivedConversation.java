package com.wakilfly.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User can archive a conversation (hide from main list). Per user + other participant.
 */
@Entity
@Table(name = "user_archived_conversations", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "other_user_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserArchivedConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "other_user_id", nullable = false)
    private User otherUser;

    @CreationTimestamp
    @Column(name = "archived_at", updatable = false)
    private LocalDateTime archivedAt;
}
