package com.wakilfly.repository;

import com.wakilfly.model.Post;
import com.wakilfly.model.PostType;
import com.wakilfly.model.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    // Feed for a user (posts from people they follow + their own). Excludes blocked users. Restricted users only see public posts from restricters.
    @Query("SELECT p FROM Post p WHERE p.isDeleted = false AND " +
            "p.author NOT IN (SELECT ub.blocked FROM UserBlock ub WHERE ub.blocker.id = :userId) AND " +
            "p.author NOT IN (SELECT ub.blocker FROM UserBlock ub WHERE ub.blocked.id = :userId) AND " +
            "(p.author.id NOT IN (SELECT ur.restricter.id FROM UserRestriction ur WHERE ur.restricted.id = :userId) OR p.visibility = 'PUBLIC') AND " +
            "(p.author.id = :userId OR " +
            "(p.author IN (SELECT f FROM User u JOIN u.following f WHERE u.id = :userId) AND " +
            "(p.visibility = 'PUBLIC' OR " +
            "(p.visibility = 'FOLLOWERS') OR " +
            "(p.visibility = 'FRIENDS' AND EXISTS (" +
            "SELECT fs FROM Friendship fs WHERE fs.status = 'ACCEPTED' AND " +
            "((fs.requester.id = :userId AND fs.addressee = p.author) OR (fs.addressee.id = :userId AND fs.requester = p.author))" +
            "))))) " +
            "ORDER BY p.createdAt DESC")
    Page<Post> findFeedForUser(@Param("userId") UUID userId, Pageable pageable);

    /** Same as findFeedForUser but only posts since given time (for algorithm: fetch candidates then score in app). Excludes STORY/REEL. */
    @Query("SELECT p FROM Post p WHERE p.isDeleted = false AND p.postType = 'POST' AND p.createdAt >= :since AND " +
            "p.author NOT IN (SELECT ub.blocked FROM UserBlock ub WHERE ub.blocker.id = :userId) AND " +
            "p.author NOT IN (SELECT ub.blocker FROM UserBlock ub WHERE ub.blocked.id = :userId) AND " +
            "(p.author.id NOT IN (SELECT ur.restricter.id FROM UserRestriction ur WHERE ur.restricted.id = :userId) OR p.visibility = 'PUBLIC') AND " +
            "(p.author.id = :userId OR " +
            "(p.author IN (SELECT f FROM User u JOIN u.following f WHERE u.id = :userId) AND " +
            "(p.visibility = 'PUBLIC' OR p.visibility = 'FOLLOWERS' OR " +
            "(p.visibility = 'FRIENDS' AND EXISTS (SELECT fs FROM Friendship fs WHERE fs.status = 'ACCEPTED' AND " +
            "((fs.requester.id = :userId AND fs.addressee = p.author) OR (fs.addressee.id = :userId AND fs.requester = p.author))))) " +
            ")) ORDER BY p.createdAt DESC")
    List<Post> findFeedCandidatesSince(@Param("userId") UUID userId, @Param("since") LocalDateTime since, Pageable pageable);

    // Public feed (for visitors/explore)
    @Query("SELECT p FROM Post p WHERE p.isDeleted = false AND p.visibility = :visibility ORDER BY p.createdAt DESC")
    Page<Post> findByVisibility(@Param("visibility") Visibility visibility, Pageable pageable);

    // Posts by user
    @Query("SELECT p FROM Post p WHERE p.author.id = :userId AND p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Post> findByAuthorId(@Param("userId") UUID userId, Pageable pageable);

    // Posts by type (reels)
    @Query("SELECT p FROM Post p WHERE p.postType = :postType AND p.isDeleted = false AND p.visibility = 'PUBLIC' ORDER BY p.createdAt DESC")
    Page<Post> findByPostType(@Param("postType") PostType postType, Pageable pageable);

    /** Reels feed candidates: from me + following + public; for scoring by engagement + recency. */
    @Query("SELECT p FROM Post p WHERE p.postType = 'REEL' AND p.isDeleted = false AND " +
            "p.author NOT IN (SELECT ub.blocked FROM UserBlock ub WHERE ub.blocker.id = :userId) AND " +
            "p.author NOT IN (SELECT ub.blocker FROM UserBlock ub WHERE ub.blocked.id = :userId) AND " +
            "(p.author.id = :userId OR p.author IN (SELECT f FROM User u JOIN u.following f WHERE u.id = :userId) OR p.visibility = 'PUBLIC') " +
            "ORDER BY p.createdAt DESC")
    List<Post> findReelsCandidatesForUser(@Param("userId") UUID userId, Pageable pageable);

    /** (postId, hashtagName) for given posts (for Reels personalization). */
    @Query(value = "SELECT ph.post_id AS id, h.name FROM post_hashtags ph INNER JOIN hashtags h ON h.id = ph.hashtag_id WHERE ph.post_id IN (:postIds)", nativeQuery = true)
    List<Object[]> findPostIdAndHashtagNameByPostIdIn(@Param("postIds") List<UUID> postIds);

    // Trending posts (by reactions + comments)
    @Query("SELECT p FROM Post p WHERE p.isDeleted = false AND p.visibility = 'PUBLIC' ORDER BY SIZE(p.reactions) DESC, SIZE(p.comments) DESC")
    Page<Post> findTrending(Pageable pageable);

    // Search posts by caption
    @Query("SELECT p FROM Post p WHERE p.isDeleted = false AND p.visibility = 'PUBLIC' AND LOWER(p.caption) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Post> searchPosts(@Param("query") String query, Pageable pageable);

    // Count user posts
    long countByAuthorIdAndIsDeletedFalse(UUID authorId);

    // Posts with product tags (social commerce discovery)
    @Query("SELECT DISTINCT p FROM Post p JOIN p.productTags pt WHERE p.isDeleted = false AND p.visibility = 'PUBLIC' ORDER BY p.createdAt DESC")
    Page<Post> findPostsWithProductTags(Pageable pageable);

    // Posts tagging a specific product
    @Query("SELECT p FROM Post p JOIN p.productTags pt WHERE pt.id = :productId AND p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Post> findByProductId(@Param("productId") UUID productId, Pageable pageable);

    // Posts in a community (group/channel): pinned first, then by recency
    @Query("SELECT p FROM Post p WHERE p.community.id = :communityId AND p.isDeleted = false ORDER BY p.isPinned DESC, p.createdAt DESC")
    Page<Post> findByCommunityId(@Param("communityId") UUID communityId, Pageable pageable);

    long countByIsDeletedFalse();

    // Stories (Status) - Active in last 24 hours. Excludes blocked users.
    @Query("SELECT p FROM Post p WHERE p.postType = 'STORY' AND p.isDeleted = false AND p.createdAt >= :since AND " +
            "p.author NOT IN (SELECT ub.blocked FROM UserBlock ub WHERE ub.blocker.id = :userId) AND " +
            "p.author NOT IN (SELECT ub.blocker FROM UserBlock ub WHERE ub.blocked.id = :userId) AND " +
            "(p.author.id = :userId OR " +
            "(p.author IN (SELECT f FROM User u JOIN u.following f WHERE u.id = :userId) AND " +
            "(p.visibility = 'PUBLIC' OR p.visibility = 'FRIENDS'))) " +
            "ORDER BY p.createdAt DESC")
    List<Post> findActiveStories(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
}
