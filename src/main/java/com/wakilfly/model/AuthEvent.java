package com.wakilfly.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Background data captured at registration/login (like Facebook/Instagram).
 * - IP address, device, browser, OS, approximate location
 * - Used for security, fraud detection, and "login activity" for user
 */
@Entity
@Table(name = "auth_events", indexes = {
        @Index(name = "idx_auth_events_user_id", columnList = "user_id"),
        @Index(name = "idx_auth_events_created_at", columnList = "created_at"),
        @Index(name = "idx_auth_events_ip", columnList = "ip_address")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private AuthEventType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Identifier used (phone/email) - for failed login when user is unknown */
    @Column(name = "identifier", length = 255)
    private String identifier;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "browser", length = 100)
    private String browser;

    @Column(name = "os", length = 100)
    private String os;

    @Column(name = "accept_language", length = 200)
    private String acceptLanguage;

    @Column(name = "country_from_ip", length = 10)
    private String countryFromIp;

    @Column(name = "timezone", length = 100)
    private String timezone;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(name = "success")
    private Boolean success;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
