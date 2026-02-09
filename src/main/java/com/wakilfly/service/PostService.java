package com.wakilfly.service;

import com.wakilfly.dto.request.CreateCommentRequest;
import com.wakilfly.dto.request.CreatePostRequest;
import com.wakilfly.dto.response.CommentResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.PostResponse;
import com.wakilfly.model.*;
import com.wakilfly.model.SavedPost;
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

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.wakilfly.model.Community;
import com.wakilfly.repository.CommunityRepository;
import com.wakilfly.repository.CommunityMemberRepository;
import com.wakilfly.repository.SavedPostRepository;
import com.wakilfly.repository.HashtagRepository;
import com.wakilfly.repository.StoryViewRepository;
import com.wakilfly.repository.UserBlockRepository;
import com.wakilfly.model.NotificationType;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

        private final PostRepository postRepository;
        private final UserRepository userRepository;
        private final CommentRepository commentRepository;
        private final PostMediaRepository postMediaRepository;
        private final ProductRepository productRepository;
        private final FileStorageService fileStorageService;
        private final PostReactionRepository postReactionRepository;
        private final CommunityRepository communityRepository;
        private final CommunityMemberRepository communityMemberRepository;
        private final NotificationService notificationService;
        private final SavedPostRepository savedPostRepository;
        private final HashtagRepository hashtagRepository;
        private final StoryViewRepository storyViewRepository;
        private final UserBlockRepository userBlockRepository;

        private static final Pattern HASHTAG_PATTERN = Pattern.compile("#([\\w\\u0080-\\uFFFF]+)");

        @Transactional
        public PostResponse createPost(UUID userId, CreatePostRequest request, List<MultipartFile> files) {
                User author = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

                // Check for Community Post
                Community community = null;
                if (request.getCommunityId() != null) {
                        community = communityRepository.findById(request.getCommunityId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Community", "id",
                                                        request.getCommunityId()));

                        // Validate membership
                        if (!communityMemberRepository.existsByCommunityIdAndUserId(community.getId(),
                                        author.getId())) {
                                throw new BadRequestException("You are not a member of this community");
                        }

                        // If only creator/admins can post, enforce it
                        if (Boolean.FALSE.equals(community.getAllowMemberPosts())) {
                                boolean isCreator = community.getCreator().getId().equals(author.getId());
                                boolean isAdmin = communityMemberRepository
                                                .findByCommunityIdAndUserId(community.getId(), author.getId())
                                                .map(m -> m.getRole() == com.wakilfly.model.CommunityRole.ADMIN)
                                                .orElse(false);
                                if (!isCreator && !isAdmin) {
                                        throw new BadRequestException("Only the group creator and admins can post in this group");
                                }
                        }
                }

                Post.PostBuilder postBuilder = Post.builder()
                                .caption(request.getCaption())
                                .author(author)
                                .community(community) // Set community
                                .visibility(request.getVisibility() != null ? request.getVisibility()
                                                : Visibility.PUBLIC)
                                .postType(request.getPostType() != null ? request.getPostType() : PostType.POST);

                // Handle Shared Post (Repost - Facebook Repost feature)
                if (request.getOriginalPostId() != null) {
                        Post originalPost = postRepository.findById(request.getOriginalPostId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Post", "id",
                                                        request.getOriginalPostId()));

                        // If reposting a repost, link to the absolute original to avoid chain
                        if (originalPost.getOriginalPost() != null) {
                                originalPost = originalPost.getOriginalPost();
                        }

                        postBuilder.originalPost(originalPost);

                        // Increment share count on original post
                        originalPost.setSharesCount(originalPost.getSharesCount() + 1);
                        postRepository.save(originalPost);
                }

                Post post = postBuilder.build();
                post = postRepository.save(post);

                // Add product tags
                if (request.getProductTags() != null && !request.getProductTags().isEmpty()) {
                        Post finalPost = post;
                        for (UUID productId : request.getProductTags()) {
                                productRepository.findById(productId).ifPresent(product -> {
                                        finalPost.getProductTags().add(product);
                                });
                        }
                        postRepository.save(finalPost);
                }

                // Save media files: either from pre-uploaded URLs (chunked) or from multipart files
                if (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) {
                        int order = 0;
                        for (String url : request.getMediaUrls()) {
                                if (url == null || url.isBlank()) continue;
                                MediaType type = isVideoUrl(url) ? MediaType.VIDEO : MediaType.IMAGE;
                                PostMedia media = PostMedia.builder()
                                                .post(post)
                                                .url(url.trim())
                                                .type(type)
                                                .displayOrder(order++)
                                                .build();
                                postMediaRepository.save(media);
                        }
                        if (!post.getMedia().isEmpty()) postRepository.save(post);
                } else if (files != null && !files.isEmpty()) {
                        int order = 0;
                        for (MultipartFile file : files) {
                                String url = fileStorageService.storeFile(file, "posts");
                                MediaType type = file.getContentType() != null
                                                && file.getContentType().startsWith("video")
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

                // Validation for REEL: Must have at least one video
                if (post.getPostType() == PostType.REEL && !post.getMedia().isEmpty()) {
                        boolean hasVideo = post.getMedia().stream()
                                        .anyMatch(m -> m.getType() == MediaType.VIDEO);
                        if (!hasVideo) {
                                log.warn("Post of type REEL created without video for user {}", userId);
                        }
                }

                // Parse and link hashtags from caption (#mzumbe #darasalaam)
                if (request.getCaption() != null && !request.getCaption().isEmpty()) {
                        Set<String> tagNames = parseHashtagsFromCaption(request.getCaption());
                        for (String name : tagNames) {
                                String normalized = name.toLowerCase();
                                Hashtag tag = hashtagRepository.findByNameIgnoreCase(normalized)
                                                .orElseGet(() -> hashtagRepository.save(Hashtag.builder().name(normalized).build()));
                                post.getHashtags().add(tag);
                        }
                        if (!post.getHashtags().isEmpty()) {
                                postRepository.save(post);
                        }
                }

                return mapToPostResponse(post, userId);
        }

        private static boolean isVideoUrl(String url) {
                if (url == null) return false;
                String lower = url.toLowerCase();
                return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".webm")
                                || lower.endsWith(".m4v") || lower.endsWith(".avi") || lower.contains(".mp4?");
        }

        private Set<String> parseHashtagsFromCaption(String caption) {
                Set<String> out = new LinkedHashSet<>();
                Matcher m = HASHTAG_PATTERN.matcher(caption);
                while (m.find()) {
                        String tag = m.group(1);
                        if (tag.length() <= 100) out.add(tag);
                }
                return out;
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
                if (currentUserId != null && isBlockedBetween(currentUserId, userId)) {
                        return PagedResponse.<PostResponse>builder()
                                        .content(List.of())
                                        .page(page)
                                        .size(size)
                                        .totalElements(0)
                                        .totalPages(0)
                                        .last(true)
                                        .first(true)
                                        .build();
                }
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

        public List<PostResponse> getActiveStories(UUID userId) {
                LocalDateTime since = LocalDateTime.now().minusHours(24);
                List<Post> stories = postRepository.findActiveStories(userId, since);
                return stories.stream()
                                .map(post -> mapToPostResponse(post, userId))
                                .collect(Collectors.toList());
        }

        public PostResponse getPostById(UUID postId, UUID currentUserId) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

                if (post.getIsDeleted()) {
                        throw new ResourceNotFoundException("Post not found");
                }
                if (currentUserId != null && isBlockedBetween(currentUserId, post.getAuthor().getId())) {
                        throw new ResourceNotFoundException("Post not found");
                }

                return mapToPostResponse(post, currentUserId);
        }

        public PagedResponse<PostResponse> getPostsByCommunity(UUID communityId, int page, int size, UUID currentUserId) {
                Community community = communityRepository.findById(communityId)
                                .orElseThrow(() -> new ResourceNotFoundException("Community", "id", communityId));
                boolean isMember = currentUserId != null
                                && communityMemberRepository.existsByCommunityIdAndUserId(communityId, currentUserId);
                if (community.getPrivacy() == Visibility.PRIVATE && !isMember) {
                        throw new ResourceNotFoundException("Community", "id", communityId);
                }
                Pageable pageable = PageRequest.of(page, size);
                Page<Post> posts = postRepository.findByCommunityId(communityId, pageable);
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

        private boolean isBlockedBetween(UUID user1, UUID user2) {
                return userBlockRepository.existsByBlockerIdAndBlockedId(user1, user2)
                                || userBlockRepository.existsByBlockerIdAndBlockedId(user2, user1);
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

        // Facebook Style Reactions (Replaces simple Like)
        @Transactional
        public int reactToPost(UUID postId, UUID userId, ReactionType type) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

                // Check if already reacted
                Optional<PostReaction> existingReaction = postReactionRepository.findByPostAndUser(post, user);

                if (existingReaction.isPresent()) {
                        // Update reaction type if different
                        PostReaction reaction = existingReaction.get();
                        if (reaction.getType() != type) {
                                reaction.setType(type);
                                postReactionRepository.save(reaction);
                        }
                } else {
                        // Create new reaction
                        PostReaction reaction = PostReaction.builder()
                                        .post(post)
                                        .user(user)
                                        .type(type)
                                        .build();
                        postReactionRepository.save(reaction);

                        // Note: No need to manually add to list if cascade works, but good for local
                        // consistency
                        // post.getReactions().add(reaction);
                        // postRepository.save(post);

                        // Notification Logic
                        log.info("User {} reacted with {} on post {}", userId, type, postId);
                        notificationService.sendNotification(post.getAuthor(), user, NotificationType.LIKE,
                                        post.getId(), user.getName() + " liked your post");
                }

                return (int) postReactionRepository.countByPost(post);
        }

        @Transactional
        public int unreactToPost(UUID postId, UUID userId) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

                Optional<PostReaction> reaction = postReactionRepository.findByPostAndUser(post, user);

                if (reaction.isPresent()) {
                        postReactionRepository.delete(reaction.get());
                }

                return (int) postReactionRepository.countByPost(post);
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
                                        .orElseThrow(() -> new ResourceNotFoundException("Comment", "id",
                                                        request.getParentId()));
                }

                Comment comment = Comment.builder()
                                .content(request.getContent())
                                .post(post)
                                .author(author)
                                .parent(parent)
                                .build();

                comment = commentRepository.save(comment);

                // Create notification
                log.info("User {} commented on post {}", userId, postId);
                notificationService.sendNotification(post.getAuthor(), author, NotificationType.COMMENT, post.getId(),
                                author.getName() + " commented on your post");

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
                String userReaction = null;

                // Get reaction count directly from DB or size (DB is safer for lazy loading
                // issues)
                int reactionsCount = post.getReactions() != null ? post.getReactions().size() : 0;

                if (currentUserId != null) {
                        User currentUser = userRepository.getReferenceById(currentUserId); // Light reference
                        // Check if user reacted (This might N+1 without optimization, but functional
                        // for now)
                        Optional<PostReaction> reaction = postReactionRepository.findByPostAndUser(post, currentUser);
                        if (reaction.isPresent()) {
                                userReaction = reaction.get().getType().name();
                        }
                }

                List<PostMedia> mediaList = postMediaRepository.findByPostIdOrderByDisplayOrderAsc(post.getId());

                // Recursively map original post if exists (1 level deep to avoid cycles/heavy
                // load)
                PostResponse originalPostResponse = null;
                if (post.getOriginalPost() != null) {
                        originalPostResponse = mapToPostResponseInternal(post.getOriginalPost(), currentUserId, true);
                }

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
                                .productTags(post.getProductTags().stream()
                                                .map(p -> PostResponse.ProductSummary.builder()
                                                                .id(p.getId())
                                                                .name(p.getName())
                                                                .price(p.getPrice().doubleValue())
                                                                .thumbnail(p.getThumbnail())
                                                                .build())
                                                .collect(Collectors.toList()))
                                .reactionsCount(reactionsCount)
                                .commentsCount(post.getCommentsCount())
                                .sharesCount(post.getSharesCount())
                                .viewsCount(post.getViewsCount())
                                .userReaction(userReaction)
                                .authorIsFollowed(currentUserId != null && !post.getAuthor().getId().equals(currentUserId)
                                        && userRepository.isFollowing(currentUserId, post.getAuthor().getId()))
                                .saved(currentUserId != null && savedPostRepository.existsByUserIdAndPostId(currentUserId, post.getId()))
                                .hashtags(post.getHashtags() != null ? post.getHashtags().stream().map(Hashtag::getName).collect(Collectors.toList()) : List.of())
                                .originalPost(originalPostResponse)
                                .createdAt(post.getCreatedAt())
                                .build();
        }

        // Helper to avoid infinite recursion when mapping shared post of a shared post
        // (though we flat it at creation)
        private PostResponse mapToPostResponseInternal(Post post, UUID currentUserId, boolean isShared) {
                // Simplified mapping for nested posts (no comments, no nested shares to avoid
                // loops)
                List<PostMedia> mediaList = postMediaRepository.findByPostIdOrderByDisplayOrderAsc(post.getId());

                return PostResponse.builder()
                                .id(post.getId())
                                .caption(post.getCaption())
                                .author(PostResponse.UserSummary.builder()
                                                .id(post.getAuthor().getId())
                                                .name(post.getAuthor().getName())
                                                .profilePic(post.getAuthor().getProfilePic())
                                                .build())
                                .postType(post.getPostType())
                                .media(mediaList.stream()
                                                .map(m -> PostResponse.MediaResponse.builder()
                                                                .id(m.getId())
                                                                .url(m.getUrl())
                                                                .type(m.getType().name())
                                                                .thumbnailUrl(m.getThumbnailUrl())
                                                                .build())
                                                .collect(Collectors.toList()))
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

        /**
         * Tag a product in a post
         */
        @Transactional
        public PostResponse tagProduct(UUID postId, UUID productId, UUID userId) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

                if (!post.getAuthor().getId().equals(userId)) {
                        throw new BadRequestException("You can only tag products in your own posts");
                }

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

                if (!product.getIsActive()) {
                        throw new BadRequestException("Product is not available");
                }

                post.getProductTags().add(product);
                post = postRepository.save(post);

                log.info("Product {} tagged in post {} by user {}", productId, postId, userId);

                return mapToPostResponse(post, userId);
        }

        /**
         * Remove product tag from post
         */
        @Transactional
        public PostResponse untagProduct(UUID postId, UUID productId, UUID userId) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

                if (!post.getAuthor().getId().equals(userId)) {
                        throw new BadRequestException("You can only untag products from your own posts");
                }

                post.getProductTags().removeIf(p -> p.getId().equals(productId));
                post = postRepository.save(post);

                log.info("Product {} untagged from post {} by user {}", productId, postId, userId);

                return mapToPostResponse(post, userId);
        }

        /**
         * Get posts with product tags (for marketplace discovery)
         */
        public PagedResponse<PostResponse> getPostsWithProductTags(int page, int size, UUID currentUserId) {
                Pageable pageable = PageRequest.of(page, size);
                Page<Post> posts = postRepository.findPostsWithProductTags(pageable);

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

        // ==================== SAVE POST (Hifadhi) ====================

        @Transactional
        public void savePost(UUID postId, UUID userId) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                if (savedPostRepository.existsByUserIdAndPostId(userId, postId)) {
                        return; // already saved
                }
                SavedPost saved = SavedPost.builder().user(user).post(post).build();
                savedPostRepository.save(saved);
        }

        @Transactional
        public void unsavePost(UUID postId, UUID userId) {
                savedPostRepository.deleteByUserIdAndPostId(userId, postId);
        }

        public PagedResponse<PostResponse> getSavedPosts(UUID userId, int page, int size) {
                Pageable pageable = PageRequest.of(page, size);
                Page<SavedPost> savedPage = savedPostRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
                List<PostResponse> content = savedPage.getContent().stream()
                                .map(sp -> mapToPostResponse(sp.getPost(), userId))
                                .collect(Collectors.toList());
                return PagedResponse.<PostResponse>builder()
                                .content(content)
                                .page(savedPage.getNumber())
                                .size(savedPage.getSize())
                                .totalElements(savedPage.getTotalElements())
                                .totalPages(savedPage.getTotalPages())
                                .last(savedPage.isLast())
                                .first(savedPage.isFirst())
                                .build();
        }

        /**
         * Share a post to story (24h). Creates a STORY post with originalPost reference.
         * Repost to feed: use createPost with originalPostId and postType=POST.
         */
        @Transactional
        public PostResponse sharePostToStory(UUID originalPostId, UUID userId, String caption) {
                CreatePostRequest request = CreatePostRequest.builder()
                                .caption(caption)
                                .postType(PostType.STORY)
                                .originalPostId(originalPostId)
                                .visibility(Visibility.PUBLIC)
                                .build();
                return createPost(userId, request, null);
        }

        /** Explore: posts by hashtag (#tagName without #) */
        public PagedResponse<PostResponse> getPostsByHashtag(String tagName, int page, int size, UUID currentUserId) {
                Pageable pageable = PageRequest.of(page, size);
                String name = tagName.startsWith("#") ? tagName.substring(1) : tagName;
                Page<Post> posts = hashtagRepository.findPostsByHashtagName(name, pageable);
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

        /** Trending hashtags for Explore tab */
        public List<String> getTrendingHashtags(int page, int size) {
                Pageable pageable = PageRequest.of(page, size);
                return hashtagRepository.findTrendingHashtags(pageable).getContent().stream()
                                .map(Hashtag::getName)
                                .collect(Collectors.toList());
        }

        // ==================== STORY VIEWERS (Who viewed my story) ====================

        @Transactional
        public void recordStoryView(UUID postId, UUID viewerUserId) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
                if (post.getPostType() != PostType.STORY) {
                        throw new BadRequestException("Only story posts can record views");
                }
                User viewer = userRepository.findById(viewerUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", viewerUserId));
                if (post.getAuthor().getId().equals(viewerUserId)) {
                        return; // don't record self view
                }
                if (storyViewRepository.existsByViewerIdAndPostId(viewerUserId, postId)) {
                        return; // already viewed
                }
                StoryView sv = StoryView.builder().viewer(viewer).post(post).build();
                storyViewRepository.save(sv);
                // Optionally increment viewsCount on post
                post.setViewsCount(post.getViewsCount() + 1);
                postRepository.save(post);
        }

        public PagedResponse<PostResponse.UserSummary> getStoryViewers(UUID postId, UUID currentUserId, int page, int size) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
                if (post.getPostType() != PostType.STORY) {
                        throw new BadRequestException("Only story posts have viewers list");
                }
                if (!post.getAuthor().getId().equals(currentUserId)) {
                        throw new BadRequestException("Only the story author can see who viewed");
                }
                Pageable pageable = PageRequest.of(page, size);
                Page<StoryView> views = storyViewRepository.findByPostIdOrderByViewedAtDesc(postId, pageable);
                List<PostResponse.UserSummary> content = views.getContent().stream()
                                .map(sv -> PostResponse.UserSummary.builder()
                                                .id(sv.getViewer().getId())
                                                .name(sv.getViewer().getName())
                                                .profilePic(sv.getViewer().getProfilePic())
                                                .build())
                                .collect(Collectors.toList());
                return PagedResponse.<PostResponse.UserSummary>builder()
                                .content(content)
                                .page(views.getNumber())
                                .size(views.getSize())
                                .totalElements(views.getTotalElements())
                                .totalPages(views.getTotalPages())
                                .last(views.isLast())
                                .first(views.isFirst())
                                .build();
        }
}
