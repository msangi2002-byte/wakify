package com.wakilfly.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    @Column(unique = true, nullable = false)
    private String phone;

    @Column(nullable = false)
    private String password;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "profile_pic")
    private String profilePic;

    @Column(name = "cover_pic")
    private String coverPic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "onboarding_completed")
    @Builder.Default
    private Boolean onboardingCompleted = false;

    @Column(name = "otp_code")
    private String otpCode;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    // --- Extended Social Profile Details ---

    private String work; // e.g. "Software Engineer at Google"

    private String education; // e.g. "Studied at UDSM"

    @Column(name = "current_city")
    private String currentCity; // e.g. "Dar es Salaam"

    private String region; // Mkoa e.g. "Dar es Salaam", "Mwanza"

    private String country; // Taifa e.g. "Tanzania"

    @Column(columnDefinition = "TEXT")
    private String interests; // Hobbies/vitaka comma-separated e.g. "Music,Sports,Tech"

    private String hometown; // e.g. "Arusha"

    @Column(name = "relationship_status")
    private String relationshipStatus; // e.g. "Single", "Married"

    private String gender; // "Male", "Female", "Other"

    @Column(name = "date_of_birth")
    private java.time.LocalDate dateOfBirth;

    private String website;

    /** Who can see your profile (PUBLIC, FOLLOWERS, PRIVATE) */
    @Enumerated(EnumType.STRING)
    @Column(name = "profile_visibility", length = 20)
    @Builder.Default
    private Visibility profileVisibility = Visibility.PUBLIC;

    /** Who can see your following/followers list (PUBLIC, FOLLOWERS, PRIVATE) */
    @Enumerated(EnumType.STRING)
    @Column(name = "following_list_visibility", length = 20)
    @Builder.Default
    private Visibility followingListVisibility = Visibility.PUBLIC;

    // Referral tracking
    @Column(name = "referred_by_agent_code")
    private String referredByAgentCode; // Agent code who referred this user

    // ---------------------------------------

    // Followers: users who follow this user
    @ManyToMany
    @JoinTable(name = "follows", joinColumns = @JoinColumn(name = "following_id"), inverseJoinColumns = @JoinColumn(name = "follower_id"))
    @Builder.Default
    private Set<User> followers = new HashSet<>();

    // Following: users this user follows
    @ManyToMany(mappedBy = "followers")
    @Builder.Default
    private Set<User> following = new HashSet<>();

    // Posts by this user
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Post> posts = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserWallet wallet;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Last activity/heartbeat; used to compute isOnline (within ~5 min). */
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    // Helper methods
    public void addFollower(User follower) {
        this.followers.add(follower);
    }

    public void removeFollower(User follower) {
        this.followers.remove(follower);
    }

    public int getFollowersCount() {
        return followers != null ? followers.size() : 0;
    }

    public int getFollowingCount() {
        return following != null ? following.size() : 0;
    }

    public int getPostsCount() {
        return posts != null ? posts.size() : 0;
    }
}
