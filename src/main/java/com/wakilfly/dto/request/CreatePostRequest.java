package com.wakilfly.dto.request;

import com.wakilfly.model.PostType;
import com.wakilfly.model.Visibility;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {

    @Size(max = 2000, message = "Caption cannot exceed 2000 characters")
    private String caption;

    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    @Builder.Default
    private PostType postType = PostType.POST;

    private List<UUID> productTags;

    private UUID originalPostId; // For sharing/reposting

    private UUID communityId; // For posting in groups/channels

    /** Location/place (e.g. "Dar es Salaam") */
    private String location;

    /** Feeling or activity (e.g. "Feeling happy", "Watching TV") */
    private String feelingActivity;

    /** User IDs to tag/mention in the post */
    private List<UUID> taggedUserIds;

    /**
     * Pre-uploaded media URLs (from chunked upload).
     * Use this when files were uploaded via POST /api/v1/upload/chunk + complete.
     * If provided, "files" in create post are ignored.
     */
    private List<String> mediaUrls;

    /**
     * Thumbnail URLs (same order as mediaUrls). Used for video media - when chunked
     * upload returns thumbnailUrl, pass it here so video story cards show the cover.
     */
    private List<String> thumbnailUrls;
}
