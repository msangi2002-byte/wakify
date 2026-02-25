package com.wakilfly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** One item for the sidebar "Sponsored" block – from an active boost (POST promotion). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SidebarSponsoredResponse {
    private UUID id;
    private String title;
    private String description;
    /** Cover image URL (thumbnail for video, or first image). */
    private String imageUrl;
    /** When post is video and imageUrl is null, UI can use this to show first frame as cover (like reels/story). */
    private String videoUrl;
    private String targetUrl;
    private LocalDateTime createdAt;
}
