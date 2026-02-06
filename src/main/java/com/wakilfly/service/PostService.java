package com.wakilfly.service;

import com.wakilfly.dto.request.CreateCommentRequest;
import com.wakilfly.dto.request.CreatePostRequest;
import com.wakilfly.dto.response.CommentResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.PostResponse;
import com.wakilfly.model.*;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PostMediaRepository postMediaRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public PostResponse createPost(UUID userId, CreatePostRequest request, List<MultipartFile> files) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Post post = Post.builder()
                .caption(request.getCaption())
                .author(author)
                .visibility(request.getVisibility() != null ? request.getVisibility() : Visibility.PUBLIC)
                .postType(request.getPostType() != null ? request.getPostType() : PostType.POST)
                .build();

        post = postRepository.save(post);

        // Save media files
        if (files != null && !files.isEmpty()) {
            int order = 0;
            for (MultipartFile file : files) {
                String url = fileStorageService.storeFile(file, "posts");
                MediaType type = file.getContentType() != null && file.getContentType().startsWith("video")
                        ? MediaType.VIDEO
                        : MediaType.IMAGE;

                PostMedia media = PostMedia.builder()
                        .post(post)
                        .url(url)
                        .type(type)
                        .displayOrder(order++)
                        .build();
                postMediaRepository.save(media);
            }
        }

        return mapToPostResponse(post, userId);
    }

    public PagedResponse<PostResponse> getFeed(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findFeedForUser(userId, pageable);

        return PagedResponse.<PostResponse>builder()
                .content(posts.getContent().stream()
                        .map(post -> mapToPostResponse(post, userId))
                        .collect(Collectors.toList()))
                .page(posts.getNumber())
                .size(posts.getSize())
                .totalElements(posts.getTotalElements())
                .totalPages(posts.getTotalPages())
                .last(posts.isLast())
                .first(posts.isFirst())
                .build();
    }

    public PagedResponse<PostResponse> getPublicFeed(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByVisibility(Visibility.PUBLIC, pageable);

        return PagedResponse.<PostResponse>builder()
                .content(posts.getContent().stream()
                        .map(post -> mapToPostResponse(post, null))
                        .collect(Collectors.toList()))
                .page(posts.getNumber())
                .size(posts.getSize())
                .totalElements(posts.getTotalElements())
                .totalPages(posts.getTotalPages())
                .last(posts.isLast())
                .first(posts.isFirst())
                .build();
    }

    public PagedResponse<PostResponse> getTrending(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findTrending(pageable);

        return PagedResponse.<PostResponse>builder()
                .content(posts.getContent().stream()
                        .map(post -> mapToPostResponse(post, null))
                        .collect(Collectors.toList()))
                .page(posts.getNumber())
                .size(posts.getSize())
                .totalElements(posts.getTotalElements())
                .totalPages(posts.getTotalPages())
                .last(posts.isLast())
                .first(posts.isFirst())
                .build();
    }

    public PagedResponse<PostResponse> getUserPosts(UUID userId, int page, int size, UUID currentUserId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByAuthorId(userId, pageable);

        return PagedResponse.<PostResponse>builder()
                .content(posts.getContent().stream()
                        .map(post -> mapToPostResponse(post, currentUserId))
                        .collect(Collectors.toList()))
                .page(posts.getNumber())
                .size(posts.getSize())
                .totalElements(posts.getTotalElements())
                .totalPages(posts.getTotalPages())
                .last(posts.isLast())
                .first(posts.isFirst())
                .build();
    }

    public PagedResponse<PostResponse> getReels(int page, int size, UUID currentUserId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByPostType(PostType.REEL, pageable);

        return PagedResponse.<PostResponse>builder()
                .content(posts.getContent().stream()
                        .map(post -> mapToPostResponse(post, currentUserId))
                        .collect(Collectors.toList()))
                .page(posts.getNumber())
                .size(posts.getSize())
                .totalElements(posts.getTotalElements())
                .totalPages(posts.getTotalPages())
                .last(posts.isLast())
                .first(posts.isFirst())
                .build();
    }

    public PostResponse getPostById(UUID postId, UUID currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (post.getIsDeleted()) {
            throw new ResourceNotFoundException("Post not found");
        }

        return mapToPostResponse(post, currentUserId);
    }

    @Transactional
    public void deletePost(UUID postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own posts");
        }

        post.setIsDeleted(true);
        postRepository.save(post);
    }

    @Transactional
    public int likePost(UUID postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (post.isLikedBy(user)) {
            throw new BadRequestException("You already liked this post");
        }

        post.addLike(user);
        postRepository.save(post);

        // TODO: Create notification
        log.info("User {} liked post {}", userId, postId);

        return post.getLikesCount();
    }

    @Transactional
    public int unlikePost(UUID postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        post.removeLike(user);
        postRepository.save(post);

        return post.getLikesCount();
    }

    @Transactional
    public CommentResponse addComment(UUID postId, UUID userId, CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", request.getParentId()));
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .post(post)
                .author(author)
                .parent(parent)
                .build();

        comment = commentRepository.save(comment);

        // TODO: Create notification
        log.info("User {} commented on post {}", userId, postId);

        return mapToCommentResponse(comment);
    }

    public PagedResponse<CommentResponse> getPostComments(UUID postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository
                .findByPostIdAndParentIsNullAndIsDeletedFalseOrderByCreatedAtDesc(postId, pageable);

        return PagedResponse.<CommentResponse>builder()
                .content(comments.getContent().stream()
                        .map(this::mapToCommentResponse)
                        .collect(Collectors.toList()))
                .page(comments.getNumber())
                .size(comments.getSize())
                .totalElements(comments.getTotalElements())
                .totalPages(comments.getTotalPages())
                .last(comments.isLast())
                .first(comments.isFirst())
                .build();
    }

    @Transactional
    public void deleteComment(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own comments");
        }

        comment.setIsDeleted(true);
        commentRepository.save(comment);
    }

    private PostResponse mapToPostResponse(Post post, UUID currentUserId) {
        boolean isLiked = false;
        if (currentUserId != null) {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            if (currentUser != null) {
                isLiked = post.isLikedBy(currentUser);
            }
        }

        List<PostMedia> mediaList = postMediaRepository.findByPostIdOrderByDisplayOrderAsc(post.getId());

        return PostResponse.builder()
                .id(post.getId())
                .caption(post.getCaption())
                .author(PostResponse.UserSummary.builder()
                        .id(post.getAuthor().getId())
                        .name(post.getAuthor().getName())
                        .profilePic(post.getAuthor().getProfilePic())
                        .build())
                .visibility(post.getVisibility())
                .postType(post.getPostType())
                .media(mediaList.stream()
                        .map(m -> PostResponse.MediaResponse.builder()
                                .id(m.getId())
                                .url(m.getUrl())
                                .type(m.getType().name())
                                .thumbnailUrl(m.getThumbnailUrl())
                                .build())
                        .collect(Collectors.toList()))
                .productTags(new ArrayList<>()) // TODO: Implement product tags
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .sharesCount(post.getSharesCount())
                .viewsCount(post.getViewsCount())
                .isLiked(isLiked)
                .createdAt(post.getCreatedAt())
                .build();
    }

    private CommentResponse mapToCommentResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(PostResponse.UserSummary.builder()
                        .id(comment.getAuthor().getId())
                        .name(comment.getAuthor().getName())
                        .profilePic(comment.getAuthor().getProfilePic())
                        .build())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .likesCount(comment.getLikesCount())
                .repliesCount(comment.getReplies() != null ? comment.getReplies().size() : 0)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
