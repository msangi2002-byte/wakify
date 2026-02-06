package com.wakilfly.controller;

import com.wakilfly.dto.request.CreateCommentRequest;
import com.wakilfly.dto.request.CreatePostRequest;
import com.wakilfly.dto.response.*;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart(value = "data", required = false) CreatePostRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();

        if (request == null) {
            request = new CreatePostRequest();
        }

        PostResponse post = postService.createPost(userId, request, files);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post created successfully", post));
    }

    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getFeed(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<PostResponse> feed = postService.getFeed(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(feed));
    }

    @GetMapping("/public/feed")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getPublicFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<PostResponse> feed = postService.getPublicFeed(page, size);
        return ResponseEntity.ok(ApiResponse.success(feed));
    }

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getTrending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<PostResponse> trending = postService.getTrending(page, size);
        return ResponseEntity.ok(ApiResponse.success(trending));
    }

    @GetMapping("/reels")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getReels(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID currentUserId = null;
        if (userDetails != null) {
            currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        }
        PagedResponse<PostResponse> reels = postService.getReels(page, size, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(reels));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getUserPosts(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = null;
        if (userDetails != null) {
            currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        }
        PagedResponse<PostResponse> posts = postService.getUserPosts(userId, page, size, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = null;
        if (userDetails != null) {
            currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        }
        PostResponse post = postService.getPostById(postId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(post));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        postService.deletePost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success("Post deleted successfully"));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> likePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        int likesCount = postService.likePost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success("Post liked", Map.of("likesCount", likesCount)));
    }

    @DeleteMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> unlikePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        int likesCount = postService.unlikePost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success("Post unliked", Map.of("likesCount", likesCount)));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable UUID postId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateCommentRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        CommentResponse comment = postService.addComment(postId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(comment));
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<PagedResponse<CommentResponse>>> getPostComments(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<CommentResponse> comments = postService.getPostComments(postId, page, size);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        postService.deleteComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully"));
    }

    // ==================== PRODUCT TAGS ====================

    /**
     * Get posts with product tags (social commerce discovery)
     * GET /api/v1/posts/with-products
     */
    @GetMapping("/with-products")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getPostsWithProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = null;
        if (userDetails != null) {
            currentUserId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        }
        PagedResponse<PostResponse> posts = postService.getPostsWithProductTags(page, size, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    /**
     * Tag a product in a post
     * POST /api/v1/posts/{postId}/tags/{productId}
     */
    @PostMapping("/{postId}/tags/{productId}")
    public ResponseEntity<ApiResponse<PostResponse>> tagProduct(
            @PathVariable UUID postId,
            @PathVariable UUID productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PostResponse post = postService.tagProduct(postId, productId, userId);
        return ResponseEntity.ok(ApiResponse.success("Product tagged successfully", post));
    }

    /**
     * Remove product tag from post
     * DELETE /api/v1/posts/{postId}/tags/{productId}
     */
    @DeleteMapping("/{postId}/tags/{productId}")
    public ResponseEntity<ApiResponse<PostResponse>> untagProduct(
            @PathVariable UUID postId,
            @PathVariable UUID productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PostResponse post = postService.untagProduct(postId, productId, userId);
        return ResponseEntity.ok(ApiResponse.success("Product untagged successfully", post));
    }
}
