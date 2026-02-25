package com.wakilfly.service;

import com.wakilfly.dto.request.CreateCommentRequest;
import com.wakilfly.dto.request.CreatePostRequest;
import com.wakilfly.dto.request.UpdatePostRequest;
import com.wakilfly.dto.response.CommentResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.PostInsightsResponse;
import com.wakilfly.dto.response.PostResponse;
import com.wakilfly.model.*;
import com.wakilfly.model.SavedPost;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.wakilfly.model.Community;
import com.wakilfly.repository.CommentLikeRepository;
import com.wakilfly.repository.CommunityRepository;
import com.wakilfly.repository.CommunityMemberRepository;
import com.wakilfly.repository.SavedPostRepository;
import com.wakilfly.repository.HashtagRepository;
import com.wakilfly.repository.ReelViewRepository;
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
        private final CommentLikeRepository commentLikeRepository;
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
        private final ReelViewRepository reelViewRepository;
        private final VideoThumbnailService videoThumbnailService;
        private final PromotionRepository promotionRepository;

        /** Optional placeholder image URL for video media that have no thumbnail (cover like reels/story). */
        @Value("${app.video-placeholder-url:}")
        private String videoPlaceholderUrl;
        private final PromotionService promotionService;

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
                                .postType(request.getPostType() != null ? request.getPostType() : PostType.POST)
                                .location(request.getLocation())
                                .feelingActivity(request.getFeelingActivity())
                                .storyGradient(request.getStoryGradient());

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

                // Add tagged/mentioned users
                if (request.getTaggedUserIds() != null && !request.getTaggedUserIds().isEmpty()) {
                        Post finalPost = post;
                        for (UUID uid : request.getTaggedUserIds()) {
                                if (uid.equals(userId)) continue; // skip self
                                userRepository.findById(uid).ifPresent(u -> finalPost.getTaggedUsers().add(u));
                        }
                        if (!finalPost.getTaggedUsers().isEmpty()) postRepository.save(finalPost);
                }

                // Save media files: either from pre-uploaded URLs (chunked) or from multipart files
                if (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) {
                        var thumbnailUrls = request.getThumbnailUrls();
                        int order = 0;
                        for (int i = 0; i < request.getMediaUrls().size(); i++) {
                                String url = request.getMediaUrls().get(i);
                                if (url == null || url.isBlank()) continue;
                                MediaType type = isVideoUrl(url) ? MediaType.VIDEO : MediaType.IMAGE;
                                String thumbUrl = (thumbnailUrls != null && i < thumbnailUrls.size())
                                                ? thumbnailUrls.get(i)
                                                : null;
                                PostMedia media = PostMedia.builder()
                                                .post(post)
                                                .url(url.trim())
                                                .type(type)
                                                .thumbnailUrl(thumbUrl)
                                                .displayOrder(order++)
                                                .build();
                                postMediaRepository.save(media);
                        }
                        if (!post.getMedia().isEmpty()) postRepository.save(post);
                } else if (files != null && !files.isEmpty()) {
                        int order = 0;
                        for (MultipartFile file : files) {
                                MediaType type = file.getContentType() != null
                                                && file.getContentType().startsWith("video")
                                                                ? MediaType.VIDEO
                                                                : MediaType.IMAGE;

                                String url;
                                String thumbUrl = null;
                                if (type == MediaType.VIDEO) {
                                        // Save to temp, upload video + generate thumbnail
                                        Path tempPath = null;
                                        try {
                                                String ext = extension(file.getOriginalFilename());
                                                tempPath = Files.createTempFile("wakilfy-video-", ext != null ? "." + ext : ".mp4");
                                                file.transferTo(tempPath.toFile());
                                                url = fileStorageService.storeFile(tempPath.toFile(),
                                                                file.getOriginalFilename(), "posts");
                                                Path thumbPath = videoThumbnailService.extractThumbnail(tempPath.toFile());
                                                if (thumbPath != null) {
                                                        try {
                                                                String thumbName = baseName(file.getOriginalFilename()) + "_thumb.jpg";
                                                                thumbUrl = fileStorageService.storeFile(thumbPath.toFile(), thumbName, "posts");
                                                        } finally {
                                                                Files.deleteIfExists(thumbPath);
                                                        }
                                                }
                                        } catch (IOException e) {
                                                log.warn("Video thumbnail extraction failed, using video URL only: {}", e.getMessage());
                                                url = fileStorageService.storeFile(file, "posts");
                                        } finally {
                                                if (tempPath != null) {
                                                        try {
                                                                Files.deleteIfExists(tempPath);
                                                        } catch (IOException ignored) {
                                                        }
                                                }
                                        }
                                } else {
                                        url = fileStorageService.storeFile(file, "posts");
                                }

                                PostMedia media = PostMedia.builder()
                                                .post(post)
                                                .url(url)
                                                .type(type)
                                                .thumbnailUrl(thumbUrl)
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

        private static String baseName(String filename) {
                if (filename == null || !filename.contains(".")) return filename != null ? filename : "file";
                return filename.substring(0, filename.lastIndexOf('.'));
        }

        private static String extension(String filename) {
                if (filename == null || !filename.contains(".")) return null;
                return filename.substring(filename.lastIndexOf('.') + 1);
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

        /** Check if promotion should be shown to user based on audience type. */
        private boolean shouldShowPromotionToUser(Promotion p, UUID currentUserId, User currentUser) {
                String audience = p.getAudienceType() != null ? p.getAudienceType().toUpperCase() : "";
                if ("LOCAL".equals(audience)) {
                        if (currentUser == null) return false;
                        if (p.getTargetRegions() != null && !p.getTargetRegions().isBlank()) {
                                String userRegion = currentUser.getRegion();
                                if (userRegion == null || userRegion.isBlank()) return false;
                                boolean matchRegion = java.util.Arrays.stream(p.getTargetRegions().split(","))
                                        .map(String::trim).anyMatch(r -> r.equalsIgnoreCase(userRegion));
                                if (!matchRegion) return false;
                        }
                        if (p.getTargetAgeMin() != null || p.getTargetAgeMax() != null) {
                                java.time.LocalDate dob = currentUser.getDateOfBirth();
                                if (dob == null) return false;
                                int age = (int) java.time.temporal.ChronoUnit.YEARS.between(dob, java.time.LocalDate.now());
                                if (p.getTargetAgeMin() != null && age < p.getTargetAgeMin()) return false;
                                if (p.getTargetAgeMax() != null && age > p.getTargetAgeMax()) return false;
                        }
                        if (p.getTargetGender() != null && !p.getTargetGender().equalsIgnoreCase("ALL")) {
                                String ug = currentUser.getGender();
                                if (ug == null || ug.isBlank()) return false;
                                if (!ug.toUpperCase().contains(p.getTargetGender().substring(0, 1))) return false;
                        }
                        return true;
                }
                if ("AUTOMATIC".equals(audience)) {
                        Post post = postRepository.findById(p.getTargetId()).orElse(null);
                        if (post == null || post.getAuthor() == null) return true;
                        UUID authorId = post.getAuthor().getId();
                        if (userRepository.isFollowing(currentUserId, authorId)) return true;
                        User author = post.getAuthor();
                        if (currentUser != null && author.getRegion() != null && !author.getRegion().isBlank()
                                && author.getRegion().equalsIgnoreCase(currentUser.getRegion())) return true;
                        if (currentUser != null && author.getRegion() == null) return true; // No region = broader reach
                        return false;
                }
                return true; // CUSTOM or null: show to all
        }

        /** Feed algorithm: rank by interaction probability, recency, engagement, and relationship strength (like Facebook). */
        public PagedResponse<PostResponse> getFeed(UUID userId, int page, int size) {
                LocalDateTime since = LocalDateTime.now().minusDays(14);
                List<Post> candidates = postRepository.findFeedCandidatesSince(userId, since, PageRequest.of(0, 500));
                
                // Get active boosted posts to include in feed (like Facebook/Instagram sponsored posts)
                LocalDateTime now = LocalDateTime.now();
                List<Promotion> activePromotions = promotionRepository.findActivePromotions(now).stream()
                        .filter(p -> p.getType() == PromotionType.POST 
                                && p.getTargetId() != null
                                && !p.getUser().getId().equals(userId) // Don't show user's own boosted posts
                                && p.getReach() != null 
                                && (p.getImpressions() == null || p.getImpressions() < p.getReach())) // Haven't reached target yet
                        .collect(Collectors.toList());
                
                // Create a map of promotion ID to promotion for quick lookup
                Map<UUID, Promotion> promotionMap = activePromotions.stream()
                        .collect(Collectors.toMap(Promotion::getId, p -> p, (p1, p2) -> p1));
                
                // Filter promotions by audience (AUTOMATIC = followers+similar, LOCAL = targeting, else = all)
                User currentUser = userRepository.findById(userId).orElse(null);
                List<Promotion> promotionsForUser = activePromotions.stream()
                        .filter(p -> shouldShowPromotionToUser(p, userId, currentUser))
                        .collect(Collectors.toList());

                Set<UUID> candidatePostIds = candidates.stream().map(Post::getId).collect(Collectors.toSet());
                List<Post> boostedPosts = promotionsForUser.stream()
                        .map(p -> {
                            try {
                                return postRepository.findById(p.getTargetId()).orElse(null);
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(post -> post != null 
                                && !post.getIsDeleted()
                                && !post.getAuthor().getId().equals(userId)
                                && post.getVisibility() == Visibility.PUBLIC)
                        .collect(Collectors.toList());
                
                // Add boosted posts to candidates (they'll get higher priority in scoring)
                candidates.addAll(boostedPosts);
                
                if (candidates.isEmpty() && page == 0) {
                        // New users (no following): show public feed
                        Page<Post> publicPage = postRepository.findByVisibility(Visibility.PUBLIC, PageRequest.of(0, size));
                        List<Post> list = publicPage.getContent();
                        return PagedResponse.<PostResponse>builder()
                                        .content(list.stream().map(post -> mapToPostResponse(post, userId)).collect(Collectors.toList()))
                                        .page(0).size(list.size()).totalElements(publicPage.getTotalElements())
                                        .totalPages(publicPage.getTotalPages()).last(publicPage.isLast()).first(true)
                                        .build();
                }
                List<Post> scored = scoreAndSortFeedPosts(candidates, userId, boostedPosts);
                
                // Mix boosted posts more naturally into feed (like Facebook/Instagram)
                // Insert boosted posts every 3-5 regular posts for better distribution
                List<Post> mixedFeed = mixBoostedPosts(scored, boostedPosts, 3, 5);
                
                int total = mixedFeed.size();
                int from = page * size;
                int to = Math.min(from + size, total);
                List<Post> pageContent = from < total ? mixedFeed.subList(from, to) : List.of();
                
                // Track impressions for boosted posts shown in this page
                pageContent.forEach(post -> {
                    promotionsForUser.stream()
                            .filter(p -> p.getTargetId() != null && p.getTargetId().equals(post.getId()))
                            .findFirst()
                            .ifPresent(promotion -> {
                                try {
                                    promotionService.trackImpression(promotion.getId());
                                } catch (Exception e) {
                                    log.warn("Failed to track impression for promotion {}: {}", promotion.getId(), e.getMessage());
                                }
                            });
                });
                
                int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
                
                // Create promotion lookup map for posts
                Map<UUID, Promotion> postPromotionMap = promotionsForUser.stream()
                        .filter(p -> p.getTargetId() != null)
                        .collect(Collectors.toMap(Promotion::getTargetId, p -> p, (p1, p2) -> p1));
                
                return PagedResponse.<PostResponse>builder()
                                .content(pageContent.stream().map(post -> {
                                    PostResponse response = mapToPostResponse(post, userId);
                                    // Mark if post is sponsored/boosted
                                    Promotion promotion = postPromotionMap.get(post.getId());
                                    if (promotion != null) {
                                        response.setIsSponsored(true);
                                        response.setPromotionId(promotion.getId());
                                        if (promotion.getCtaLink() != null && !promotion.getCtaLink().isBlank()) {
                                            response.setSponsorCtaLink(promotion.getCtaLink());
                                        }
                                        if (promotion.getObjective() != null) {
                                            response.setSponsorObjective(promotion.getObjective().name());
                                        }
                                    }
                                    return response;
                                }).collect(Collectors.toList()))
                                .page(page).size(size).totalElements((long) total).totalPages(totalPages)
                                .last(to >= total).first(page == 0)
                                .build();
        }

        /** Score = recency decay + engagement (reactions, comments, shares) + relationship (mutual follows, my reactions/comments on author) + boost priority. */
        private List<Post> scoreAndSortFeedPosts(List<Post> candidates, UUID currentUserId, List<Post> boostedPosts) {
                Set<UUID> boostedPostIds = boostedPosts.stream().map(Post::getId).collect(Collectors.toSet());
                return candidates.stream()
                                .map(post -> {
                                        long hoursAgo = ChronoUnit.HOURS.between(post.getCreatedAt(), LocalDateTime.now());
                                        double recency = 1.0 / (1.0 + hoursAgo / 48.0);
                                        int reactions = post.getReactionsCount();
                                        int comments = post.getCommentsCount();
                                        int shares = post.getSharesCount() != null ? post.getSharesCount() : 0;
                                        double engagement = Math.log(1.0 + reactions + comments * 2.0 + shares);
                                        UUID authorId = post.getAuthor().getId();
                                        long mutual = authorId.equals(currentUserId) ? 0 : userRepository.countMutualFollowing(currentUserId, authorId);
                                        long myReactions = postReactionRepository.countByUser_IdAndPost_Author_Id(currentUserId, authorId);
                                        long myComments = commentRepository.countByAuthor_IdAndPost_Author_Id(currentUserId, authorId);
                                        double relationship = 0.5 * mutual + 0.3 * myReactions + 0.3 * myComments;
                                        
                                        // Boost priority: boosted posts get much higher score (like sponsored content)
                                        // This ensures they appear prominently in feeds, similar to Facebook/Instagram
                                        double boostMultiplier = boostedPostIds.contains(post.getId()) ? 5.0 : 1.0;
                                        
                                        // Boosted posts get priority even if relationship is weak (they're sponsored)
                                        double relationshipScore = boostedPostIds.contains(post.getId()) 
                                                ? Math.max(relationship, 1.0) // Minimum relationship score for boosted
                                                : relationship;
                                        
                                        double score = (2.0 * recency + engagement + 0.5 * relationshipScore) * boostMultiplier;
                                        return new ScoredPost(post, score);
                                })
                                .sorted(Comparator.comparingDouble((ScoredPost sp) -> sp.score).reversed())
                                .map(sp -> sp.post)
                                .collect(Collectors.toList());
        }

        private static class ScoredPost {
                final Post post;
                final double score;
                ScoredPost(Post post, double score) { this.post = post; this.score = score; }
        }

        /**
         * Mix boosted posts naturally into feed (like Facebook/Instagram sponsored posts)
         * Inserts boosted posts every N-M regular posts for better distribution
         */
        private List<Post> mixBoostedPosts(List<Post> regularPosts, List<Post> boostedPosts, int minInterval, int maxInterval) {
                if (boostedPosts.isEmpty()) {
                        return regularPosts;
                }
                
                Set<UUID> boostedIds = boostedPosts.stream().map(Post::getId).collect(Collectors.toSet());
                List<Post> regularOnly = regularPosts.stream()
                        .filter(p -> !boostedIds.contains(p.getId()))
                        .collect(Collectors.toList());
                
                List<Post> result = new java.util.ArrayList<>();
                java.util.Collections.shuffle(boostedPosts); // Randomize which boosted posts appear first
                
                int boostedIndex = 0;
                int regularIndex = 0;
                int postsSinceBoost = 0;
                int nextBoostInterval = minInterval + (int)(Math.random() * (maxInterval - minInterval + 1));
                
                while (regularIndex < regularOnly.size() || boostedIndex < boostedPosts.size()) {
                        // Insert boosted post if interval reached and we have boosted posts left
                        if (boostedIndex < boostedPosts.size() && postsSinceBoost >= nextBoostInterval) {
                                result.add(boostedPosts.get(boostedIndex++));
                                postsSinceBoost = 0;
                                nextBoostInterval = minInterval + (int)(Math.random() * (maxInterval - minInterval + 1));
                        } else if (regularIndex < regularOnly.size()) {
                                result.add(regularOnly.get(regularIndex++));
                                postsSinceBoost++;
                        } else if (boostedIndex < boostedPosts.size()) {
                                // Add remaining boosted posts at the end
                                result.add(boostedPosts.get(boostedIndex++));
                        }
                }
                
                return result;
        }

        public PagedResponse<PostResponse> getPublicFeed(int page, int size) {
                Pageable pageable = PageRequest.of(page, size);
                Page<Post> posts = postRepository.findByVisibility(Visibility.PUBLIC, pageable);
                
                // Get active boosted posts for public feed too
                LocalDateTime now = LocalDateTime.now();
                List<Promotion> activePromotions = promotionRepository.findActivePromotions(now).stream()
                        .filter(p -> p.getType() == PromotionType.POST 
                                && p.getTargetId() != null
                                && p.getReach() != null 
                                && (p.getImpressions() == null || p.getImpressions() < p.getReach()))
                        .collect(Collectors.toList());
                
                Map<UUID, Promotion> postPromotionMap = activePromotions.stream()
                        .filter(p -> p.getTargetId() != null)
                        .collect(Collectors.toMap(Promotion::getTargetId, p -> p, (p1, p2) -> p1));

                return PagedResponse.<PostResponse>builder()
                                .content(posts.getContent().stream()
                                                .map(post -> {
                                                    PostResponse response = mapToPostResponse(post, null);
                                                    Promotion promotion = postPromotionMap.get(post.getId());
                                                    if (promotion != null) {
                                                        response.setIsSponsored(true);
                                                        response.setPromotionId(promotion.getId());
                                                        if (promotion.getObjective() != null) {
                                                            response.setSponsorObjective(promotion.getObjective().name());
                                                        }
                                                    }
                                                    return response;
                                                })
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

        /** Reels feed: from following + public; ranked by watch time, completion, shares, comments, likes + recency. Includes sponsored reels (boosted REEL posts). */
        public PagedResponse<PostResponse> getReels(int page, int size, UUID currentUserId) {
                List<Post> candidates = currentUserId != null
                        ? postRepository.findReelsCandidatesForUser(currentUserId, PageRequest.of(0, 300))
                        : postRepository.findByPostType(PostType.REEL, PageRequest.of(0, 300)).getContent();
                java.util.Set<UUID> candidateIds = candidates.stream().map(Post::getId).collect(Collectors.toSet());
                LocalDateTime now = LocalDateTime.now();
                User currentUser = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;
                List<Promotion> activePromotions = promotionRepository.findActivePromotions(now).stream()
                        .filter(p -> p.getType() == PromotionType.POST && p.getTargetId() != null
                                && (currentUserId == null || !p.getUser().getId().equals(currentUserId))
                                && (p.getReach() == null || p.getImpressions() == null || p.getImpressions() < p.getReach()))
                        .collect(Collectors.toList());
                for (Promotion p : activePromotions) {
                        if (!shouldShowPromotionToUser(p, currentUserId, currentUser)) continue;
                        postRepository.findById(p.getTargetId()).filter(post -> post.getPostType() == PostType.REEL
                                        && !post.getIsDeleted() && post.getVisibility() == Visibility.PUBLIC)
                                .filter(post -> !candidateIds.contains(post.getId()))
                                .ifPresent(post -> { candidates.add(post); candidateIds.add(post.getId()); });
                }
                List<Post> scored = scoreAndSortReels(candidates, currentUserId);
                int total = scored.size();
                int from = page * size;
                int to = Math.min(from + size, total);
                List<Post> pageContent = from < total ? scored.subList(from, to) : List.of();
                java.util.Map<UUID, Promotion> postPromotionMap = activePromotions.stream()
                        .filter(p -> p.getTargetId() != null)
                        .collect(Collectors.toMap(Promotion::getTargetId, prom -> prom, (a, b) -> a));
                for (Post post : pageContent) {
                        Promotion promotion = postPromotionMap.get(post.getId());
                        if (promotion != null) {
                                try { promotionService.trackImpression(promotion.getId()); } catch (Exception e) { log.warn("Reels impression track failed: {}", e.getMessage()); }
                        }
                }
                int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
                return PagedResponse.<PostResponse>builder()
                                .content(pageContent.stream().map(post -> {
                                        PostResponse response = mapToPostResponse(post, currentUserId);
                                        Promotion promotion = postPromotionMap.get(post.getId());
                                        if (promotion != null) {
                                                response.setIsSponsored(true);
                                                response.setPromotionId(promotion.getId());
                                                if (promotion.getCtaLink() != null && !promotion.getCtaLink().isBlank())
                                                        response.setSponsorCtaLink(promotion.getCtaLink());
                                                if (promotion.getObjective() != null)
                                                        response.setSponsorObjective(promotion.getObjective().name());
                                        }
                                        return response;
                                }).collect(Collectors.toList()))
                                .page(page).size(size).totalElements((long) total).totalPages(totalPages)
                                .last(to >= total).first(page == 0)
                                .build();
        }

        /**
         * Score reels by: Watch time (0.4) + Completion (0.3) + Shares (0.2) + Comments (0.05) + Likes (0.05), recency boost, then personalization (interest match).
         */
        private List<Post> scoreAndSortReels(List<Post> candidates, UUID currentUserId) {
                if (candidates.isEmpty()) return List.of();
                List<UUID> postIds = candidates.stream().map(Post::getId).collect(Collectors.toList());
                Map<UUID, ReelViewStats> statsMap = getReelViewStatsMap(postIds);
                Map<String, Double> userInterest = currentUserId != null ? getUserInterestHashtags(currentUserId) : new HashMap<>();
                Map<UUID, Set<String>> postTags = buildPostHashtagsMap(postIds);
                Map<UUID, Long> authorReelCount = candidates.stream()
                                .collect(Collectors.groupingBy(p -> p.getAuthor().getId(), Collectors.counting()));

                int maxReactions = candidates.stream().mapToInt(Post::getReactionsCount).max().orElse(1);
                int maxComments = candidates.stream().mapToInt(Post::getCommentsCount).max().orElse(1);
                int maxShares = candidates.stream().mapToInt(p -> p.getSharesCount() != null ? p.getSharesCount() : 0).max().orElse(1);

                final int maxR = Math.max(maxReactions, 1);
                final int maxC = Math.max(maxComments, 1);
                final int maxS = Math.max(maxShares, 1);

                return candidates.stream()
                                .map(post -> {
                                        ReelViewStats stats = statsMap.getOrDefault(post.getId(), new ReelViewStats(0.0, 0.0));
                                        long hoursAgo = ChronoUnit.HOURS.between(post.getCreatedAt(), LocalDateTime.now());
                                        double recency = 1.0 / (1.0 + hoursAgo / 72.0);

                                        double watchTimeNorm = Math.min(1.0, stats.avgWatchTimeSeconds / 60.0);
                                        double completionNorm = Math.min(1.0, Math.max(0.0, stats.completionRate));
                                        int shares = post.getSharesCount() != null ? post.getSharesCount() : 0;
                                        int reactions = post.getReactionsCount();
                                        int comments = post.getCommentsCount();
                                        double sharesNorm = Math.min(1.0, (double) shares / maxS);
                                        double likesNorm = Math.min(1.0, (double) reactions / maxR);
                                        double commentsNorm = Math.min(1.0, (double) comments / maxC);

                                        double score = (watchTimeNorm * 0.4 + completionNorm * 0.3 + sharesNorm * 0.2 + commentsNorm * 0.05 + likesNorm * 0.05) * (0.5 + recency);
                                        double similarity = cosineSimilarity(userInterest, postTags.getOrDefault(post.getId(), Set.of()));
                                        score *= (1.0 + 0.4 * similarity);
                                        long authorCount = authorReelCount.getOrDefault(post.getAuthor().getId(), 1L);
                                        if (authorCount >= 2) score *= (1.0 + 0.05 * Math.min(2, authorCount - 1));
                                        return new ScoredPost(post, score);
                                })
                                .sorted(Comparator.comparingDouble((ScoredPost sp) -> sp.score).reversed())
                                .map(sp -> sp.post)
                                .collect(Collectors.toList());
        }

        private Map<String, Double> getUserInterestHashtags(UUID userId) {
                List<UUID> fromReactions = postReactionRepository.findPostIdsByUserId(userId);
                List<UUID> fromSaved = savedPostRepository.findPostIdsByUserId(userId);
                List<UUID> fromCompletedReels = reelViewRepository.findPostIdsByUserIdAndCompletedTrue(userId);
                Set<UUID> allIds = new java.util.LinkedHashSet<>();
                allIds.addAll(fromReactions);
                allIds.addAll(fromSaved);
                allIds.addAll(fromCompletedReels);
                if (allIds.isEmpty()) return new HashMap<>();
                List<Object[]> rows = postRepository.findPostIdAndHashtagNameByPostIdIn(List.copyOf(allIds));
                Map<String, Double> tagToWeight = new HashMap<>();
                for (Object[] row : rows) {
                        if (row.length < 2) continue;
                        String tag = row[1] != null ? row[1].toString().toLowerCase() : null;
                        if (tag == null || tag.isBlank()) continue;
                        tagToWeight.merge(tag, 1.0, Double::sum);
                }
                return tagToWeight;
        }

        /** Parse UUID from native query result (can be UUID, byte[] from BINARY(16), or String). */
        private UUID parseUuidFromRow(Object value) {
                if (value == null) return null;
                if (value instanceof UUID) return (UUID) value;
                if (value instanceof byte[]) {
                        byte[] bytes = (byte[]) value;
                        if (bytes.length != 16) return null;
                        ByteBuffer bb = ByteBuffer.wrap(bytes);
                        return new UUID(bb.getLong(), bb.getLong());
                }
                try {
                        return UUID.fromString(value.toString().trim());
                } catch (IllegalArgumentException e) {
                        return null;
                }
        }

        private Map<UUID, Set<String>> buildPostHashtagsMap(List<UUID> postIds) {
                if (postIds.isEmpty()) return new HashMap<>();
                List<Object[]> rows = postRepository.findPostIdAndHashtagNameByPostIdIn(postIds);
                Map<UUID, Set<String>> map = new HashMap<>();
                for (Object[] row : rows) {
                        if (row.length < 2) continue;
                        UUID postId = parseUuidFromRow(row[0]);
                        if (postId == null) continue;
                        String tag = row[1] != null ? row[1].toString().toLowerCase() : null;
                        if (tag == null || tag.isBlank()) continue;
                        map.computeIfAbsent(postId, k -> new java.util.HashSet<>()).add(tag);
                }
                return map;
        }

        private double cosineSimilarity(Map<String, Double> userVec, Set<String> postTags) {
                if (userVec.isEmpty() || postTags.isEmpty()) return 0;
                double dot = 0;
                for (String tag : postTags) {
                        dot += userVec.getOrDefault(tag, 0.0);
                }
                double normUser = 0;
                for (double v : userVec.values()) normUser += v * v;
                normUser = Math.sqrt(normUser);
                double normPost = Math.sqrt(postTags.size());
                if (normUser < 1e-9 || normPost < 1e-9) return 0;
                return Math.min(1.0, dot / (normUser * normPost));
        }

        private Map<UUID, ReelViewStats> getReelViewStatsMap(List<UUID> postIds) {
                if (postIds.isEmpty()) return new HashMap<>();
                List<Object[]> rows = reelViewRepository.getReelStatsByPostIds(postIds);
                Map<UUID, ReelViewStats> map = new HashMap<>();
                for (Object[] row : rows) {
                        if (row.length < 3) continue;
                        UUID postId = parseUuidFromRow(row[0]);
                        if (postId == null) continue;
                        double avgWatch = row[1] instanceof Number ? ((Number) row[1]).doubleValue() : 0;
                        double completionRate = row[2] instanceof Number ? ((Number) row[2]).doubleValue() : 0;
                        map.put(postId, new ReelViewStats(avgWatch, completionRate));
                }
                return map;
        }

        @Transactional
        public void recordReelView(UUID postId, UUID userId, int watchTimeSeconds, boolean completed) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
                if (post.getPostType() != PostType.REEL) {
                        throw new BadRequestException("Only reel posts can record reel views");
                }
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                ReelView rv = ReelView.builder()
                                .post(post)
                                .user(user)
                                .watchTimeSeconds(watchTimeSeconds)
                                .completed(completed)
                                .build();
                reelViewRepository.save(rv);
                post.setViewsCount((post.getViewsCount() != null ? post.getViewsCount() : 0) + 1);
                postRepository.save(post);
        }

        private static class ReelViewStats {
                final double avgWatchTimeSeconds;
                final double completionRate;
                ReelViewStats(double avgWatchTimeSeconds, double completionRate) {
                        this.avgWatchTimeSeconds = avgWatchTimeSeconds;
                        this.completionRate = completionRate;
                }
        }

        /** Stories ordered by closeness (how often I viewed this author's stories) then recency (latest first). Includes sponsored stories (boosted STORY posts). */
        public List<PostResponse> getActiveStories(UUID userId) {
                LocalDateTime since = LocalDateTime.now().minusHours(24);
                List<Post> stories = new java.util.ArrayList<>(postRepository.findActiveStories(userId, since));
                java.util.Set<UUID> storyIds = stories.stream().map(Post::getId).collect(Collectors.toSet());
                User currentUser = userId != null ? userRepository.findById(userId).orElse(null) : null;
                LocalDateTime now = LocalDateTime.now();
                List<Promotion> activePromotions = promotionRepository.findActivePromotions(now).stream()
                        .filter(p -> p.getType() == PromotionType.POST && p.getTargetId() != null
                                && !p.getUser().getId().equals(userId)
                                && (p.getReach() == null || p.getImpressions() == null || p.getImpressions() < p.getReach()))
                        .collect(Collectors.toList());
                for (Promotion p : activePromotions) {
                        if (!shouldShowPromotionToUser(p, userId, currentUser)) continue;
                        postRepository.findById(p.getTargetId())
                                .filter(post -> post.getPostType() == PostType.STORY && !post.getIsDeleted()
                                        && !post.getCreatedAt().isBefore(since))
                                .filter(post -> !storyIds.contains(post.getId()))
                                .ifPresent(post -> { stories.add(post); storyIds.add(post.getId()); });
                }
                stories.sort(Comparator
                                .comparingLong((Post p) -> storyViewRepository.countByViewer_IdAndPost_Author_Id(userId, p.getAuthor().getId())).reversed()
                                .thenComparing(Post::getCreatedAt, Comparator.reverseOrder()));
                java.util.Map<UUID, Promotion> postPromotionMap = activePromotions.stream()
                        .filter(p -> p.getTargetId() != null)
                        .collect(Collectors.toMap(Promotion::getTargetId, prom -> prom, (a, b) -> a));
                return stories.stream()
                                .map(post -> {
                                        PostResponse response = mapToPostResponse(post, userId);
                                        Promotion promotion = postPromotionMap.get(post.getId());
                                        if (promotion != null) {
                                                response.setIsSponsored(true);
                                                response.setPromotionId(promotion.getId());
                                                if (promotion.getCtaLink() != null && !promotion.getCtaLink().isBlank())
                                                        response.setSponsorCtaLink(promotion.getCtaLink());
                                                if (promotion.getObjective() != null)
                                                        response.setSponsorObjective(promotion.getObjective().name());
                                                try { promotionService.trackImpression(promotion.getId()); } catch (Exception e) { log.warn("Story impression track failed: {}", e.getMessage()); }
                                        }
                                        return response;
                                })
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

        /** Insights for a post/reel (author only). For reels includes watch time and completion rate. */
        public PostInsightsResponse getPostInsights(UUID postId, UUID currentUserId) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
                if (!post.getAuthor().getId().equals(currentUserId)) {
                        throw new BadRequestException("Only the author can view post insights");
                }
                int views = post.getViewsCount() != null ? post.getViewsCount() : 0;
                double avgWatch = 0;
                double completionRate = 0;
                if (post.getPostType() == PostType.REEL) {
                        Map<UUID, ReelViewStats> stats = getReelViewStatsMap(List.of(postId));
                        ReelViewStats s = stats.get(postId);
                        if (s != null) {
                                avgWatch = s.avgWatchTimeSeconds;
                                completionRate = s.completionRate;
                        }
                }
                return PostInsightsResponse.builder()
                                .viewsCount(views)
                                .avgWatchTimeSeconds(avgWatch)
                                .completionRate(completionRate)
                                .likesCount(post.getReactionsCount())
                                .commentsCount(post.getCommentsCount())
                                .sharesCount(post.getSharesCount() != null ? post.getSharesCount() : 0)
                                .build();
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

        @Transactional
        public PostResponse updatePost(UUID postId, UUID userId, UpdatePostRequest request) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
                if (!post.getAuthor().getId().equals(userId)) {
                        throw new BadRequestException("You can only edit your own posts");
                }
                if (Boolean.TRUE.equals(post.getIsDeleted())) {
                        throw new ResourceNotFoundException("Post not found");
                }

                if (request.getCaption() != null) post.setCaption(request.getCaption());
                if (request.getVisibility() != null) post.setVisibility(request.getVisibility());
                if (request.getLocation() != null) post.setLocation(request.getLocation());
                if (request.getFeelingActivity() != null) post.setFeelingActivity(request.getFeelingActivity());

                if (request.getTaggedUserIds() != null) {
                        post.getTaggedUsers().clear();
                        for (UUID uid : request.getTaggedUserIds()) {
                                if (uid.equals(userId)) continue;
                                userRepository.findById(uid).ifPresent(u -> post.getTaggedUsers().add(u));
                        }
                }

                if (request.getMediaUrls() != null) {
                        post.getMedia().clear();
                        postMediaRepository.deleteByPostId(post.getId());
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
                }

                Post updated = postRepository.save(post);
                return mapToPostResponse(updated, userId);
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

                return mapToCommentResponse(comment, userId);
        }

        public PagedResponse<CommentResponse> getPostComments(UUID postId, int page, int size, UUID currentUserId) {
                Pageable pageable = PageRequest.of(page, size);
                Page<Comment> comments = commentRepository
                                .findByPostIdAndParentIsNullAndIsDeletedFalseOrderByCreatedAtDesc(postId, pageable);

                return PagedResponse.<CommentResponse>builder()
                                .content(comments.getContent().stream()
                                                .map(c -> mapToCommentResponse(c, currentUserId))
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

        @Transactional
        public int likeComment(UUID commentId, UUID userId) {
                Comment comment = commentRepository.findById(commentId)
                                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

                if (commentLikeRepository.findByCommentAndUser(comment, user).isPresent()) {
                        return (int) commentLikeRepository.countByComment(comment);
                }

                CommentLike like = CommentLike.builder()
                                .comment(comment)
                                .user(user)
                                .build();
                commentLikeRepository.save(like);
                return (int) commentLikeRepository.countByComment(comment);
        }

        @Transactional
        public int unlikeComment(UUID commentId, UUID userId) {
                Comment comment = commentRepository.findById(commentId)
                                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

                commentLikeRepository.findByCommentAndUser(comment, user).ifPresent(commentLikeRepository::delete);
                return (int) commentLikeRepository.countByComment(comment);
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

                List<PostReaction> topReactions = postReactionRepository.findTopByPostIdWithUser(post.getId(), PageRequest.of(0, 3));
                List<PostResponse.UserSummary> topReactors = topReactions.stream()
                                .map(pr -> PostResponse.UserSummary.builder()
                                                .id(pr.getUser().getId())
                                                .name(pr.getUser().getName())
                                                .profilePic(pr.getUser().getProfilePic())
                                                .build())
                                .collect(Collectors.toList());

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
                                                                .thumbnailUrl(effectiveThumbnailUrl(m))
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
                                .topReactors(topReactors)
                                .commentsCount(post.getCommentsCount())
                                .sharesCount(post.getSharesCount())
                                .viewsCount(post.getViewsCount())
                                .userReaction(userReaction)
                                .authorIsFollowed(currentUserId != null && !post.getAuthor().getId().equals(currentUserId)
                                        && userRepository.isFollowing(currentUserId, post.getAuthor().getId()))
                                .saved(currentUserId != null && savedPostRepository.existsByUserIdAndPostId(currentUserId, post.getId()))
                                .hashtags(post.getHashtags() != null ? post.getHashtags().stream().map(Hashtag::getName).collect(Collectors.toList()) : List.of())
                                .location(post.getLocation())
                                .feelingActivity(post.getFeelingActivity())
                                .storyGradient(post.getStoryGradient())
                                .taggedUsers(post.getTaggedUsers() != null ? post.getTaggedUsers().stream()
                                                .map(u -> PostResponse.UserSummary.builder()
                                                                .id(u.getId())
                                                                .name(u.getName())
                                                                .profilePic(u.getProfilePic())
                                                                .build())
                                                .collect(Collectors.toList()) : List.of())
                                .originalPost(originalPostResponse)
                                .isPinned(post.getIsPinned())
                                .pinnedAt(post.getPinnedAt())
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
                                                                .thumbnailUrl(effectiveThumbnailUrl(m))
                                                                .build())
                                                .collect(Collectors.toList()))
                                .location(post.getLocation())
                                .feelingActivity(post.getFeelingActivity())
                                .storyGradient(post.getStoryGradient())
                                .createdAt(post.getCreatedAt())
                                .build();
        }

        /** For video media without thumbnail, return placeholder URL so feed/sponsored can show a cover (like reels/story). */
        private String effectiveThumbnailUrl(PostMedia m) {
                if (m.getType() == MediaType.VIDEO
                                && (m.getThumbnailUrl() == null || m.getThumbnailUrl().isBlank())
                                && videoPlaceholderUrl != null && !videoPlaceholderUrl.isBlank())
                        return videoPlaceholderUrl;
                return m.getThumbnailUrl();
        }

        private CommentResponse mapToCommentResponse(Comment comment, UUID currentUserId) {
                int likesCount = (int) commentLikeRepository.countByComment(comment);
                boolean userLiked = currentUserId != null
                                && commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), currentUserId);

                List<Comment> replyList = comment.getReplies() != null
                                ? comment.getReplies().stream()
                                                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                                                .sorted((a, b) -> (a.getCreatedAt() != null && b.getCreatedAt() != null)
                                                                ? a.getCreatedAt().compareTo(b.getCreatedAt())
                                                                : 0)
                                                .collect(Collectors.toList())
                                : List.of();

                List<CommentResponse> replies = replyList.stream()
                                .map(r -> mapToCommentResponse(r, currentUserId))
                                .collect(Collectors.toList());

                return CommentResponse.builder()
                                .id(comment.getId())
                                .content(comment.getContent())
                                .author(PostResponse.UserSummary.builder()
                                                .id(comment.getAuthor().getId())
                                                .name(comment.getAuthor().getName())
                                                .profilePic(comment.getAuthor().getProfilePic())
                                                .build())
                                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                                .likesCount(likesCount)
                                .userLiked(userLiked)
                                .repliesCount(replies.size())
                                .replies(replies)
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
