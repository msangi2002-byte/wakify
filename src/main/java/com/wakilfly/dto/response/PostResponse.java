package com.wakilfly.dto.response;

import com.wakilfly.model.PostType;
import com.wakilfly.model.Visibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private UUID id;
    private String caption;
    private UserSummary author;
    private Visibility visibility;
    private PostType postType;
    private List<MediaResponse> media;
    private List<ProductSummary> productTags;
    private Integer reactionsCount;
    /** First 2–3 users who reacted (for "X and Y others" in feed). */
    private List<UserSummary> topReactors;
    private Integer commentsCount;
    private Integer sharesCount;
    private Integer viewsCount;
    private String userReaction; // Type of reaction by current user (or null)
    private Boolean authorIsFollowed; // True if current user follows the post author (feed only)
    private Boolean saved; // True if current user has saved this post (Hifadhi)
    private List<String> hashtags; // #tag names for Explore
    private String location; // Place on post
    private String feelingActivity; // e.g. "Feeling happy"
    private List<UserSummary> taggedUsers; // Tagged/mentioned users
    private PostResponse originalPost; // For shared posts
    private Boolean isPinned; // In group: admin pinned (shown first)
    /** Text story gradient (e.g. linear-gradient(...)). Shown on story card for text stories. */
    private String storyGradient;
    private LocalDateTime pinnedAt;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private UUID id;
        private String name;
        private String profilePic;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaResponse {
        private UUID id;
        private String url;
        private String type;
        private String thumbnailUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSummary {
        private UUID id;
        private String name;
        private Double price;
        private String thumbnail;
    }
}
