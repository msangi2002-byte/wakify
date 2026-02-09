package com.wakilfly.controller;

import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.PostResponse;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Explore by hashtags (#mzumbe #darasalaam).
 */
@RestController
@RequestMapping("/api/v1/hashtags")
@RequiredArgsConstructor
public class HashtagController {

    private final PostService postService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Trending hashtags for Explore tab
     * GET /api/v1/hashtags/trending
     */
    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<String>>> getTrendingHashtags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<String> tags = postService.getTrendingHashtags(page, size);
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    /**
     * Posts by hashtag (tag without # or with #)
     * GET /api/v1/hashtags/{tagName}/posts
     */
    @GetMapping("/{tagName}/posts")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getPostsByHashtag(
            @PathVariable String tagName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = null;
        if (userDetails != null) {
            currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        }
        PagedResponse<PostResponse> posts = postService.getPostsByHashtag(tagName, page, size, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }
}
