package com.wakilfly.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Hashed contact (phone/email) uploaded by user for "People You May Know".
 * We match hash(normalize(phone)) and hash(normalize(email)) against other users.
 */
@Entity
@Table(name = "user_contact_hashes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "contact_type", "hash"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserContactHash {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false, length = 10)
    private ContactHashType contactType;

    @Column(nullable = false, length = 64)
    private String hash;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum ContactHashType {
        PHONE,
        EMAIL
    }
}
