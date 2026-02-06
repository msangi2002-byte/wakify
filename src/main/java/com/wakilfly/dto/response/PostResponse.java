package com.wakilfly.dto.response;

import com.wakilfly.entity.PostType;
import com.wakilfly.entity.Visibility;
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
    private Integer likesCount;
    private Integer commentsCount;
    private Integer sharesCount;
    private Integer viewsCount;
    private Boolean isLiked;
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
