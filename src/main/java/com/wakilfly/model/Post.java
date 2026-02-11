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
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PostType postType = PostType.POST;

    // Media files (images/videos)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostMedia> media = new ArrayList<>();

    // Reactions (Replacing old Likes)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostReaction> reactions = new ArrayList<>();

    // Shared Post Logic (Repost)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_post_id")
    private Post originalPost;

    // Comments
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    // Product tags (for social commerce)
    @ManyToMany
    @JoinTable(name = "post_product_tags", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "product_id"))
    @Builder.Default
    private Set<Product> productTags = new HashSet<>();

    // Community (Group/Channel) link. Null if personal post.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    // Hashtags (#mzumbe #darasalaam) for discovery and Explore
    @ManyToMany
    @JoinTable(name = "post_hashtags", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "hashtag_id"))
    @Builder.Default
    private Set<Hashtag> hashtags = new HashSet<>();

    /** Location/place on post (e.g. "Dar es Salaam", "Nyerere Square") */
    @Column(length = 500)
    private String location;

    /** Feeling/activity (e.g. "Feeling happy", "Watching TV") */
    @Column(name = "feeling_activity", length = 200)
    private String feelingActivity;

    /** Tagged/mentioned users in this post */
    @ManyToMany
    @JoinTable(name = "post_tagged_users", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<User> taggedUsers = new HashSet<>();

    @Column(name = "shares_count")
    @Builder.Default
    private Integer sharesCount = 0;

    @Column(name = "views_count")
    @Builder.Default
    private Integer viewsCount = 0;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    /** Pinned in group by admin (shown first in group feed) */
    @Column(name = "is_pinned")
    @Builder.Default
    private Boolean isPinned = false;

    @Column(name = "pinned_at")
    private java.time.LocalDateTime pinnedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public int getReactionsCount() {
        return reactions != null ? reactions.size() : 0;
    }

    public int getCommentsCount() {
        return comments != null ? comments.size() : 0;
    }

    public void addReaction(PostReaction reaction) {
        reactions.add(reaction);
        reaction.setPost(this);
    }

    public void removeReaction(PostReaction reaction) {
        reactions.remove(reaction);
        reaction.setPost(null);
    }

    public void addMedia(PostMedia postMedia) {
        media.add(postMedia);
        postMedia.setPost(this);
    }

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setPost(this);
    }
}
