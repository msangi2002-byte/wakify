package com.wakilfly.repository;

import com.wakilfly.entity.Post;
import com.wakilfly.entity.PostType;
import com.wakilfly.entity.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    // Feed for a user (posts from people they follow + their own)
    @Query("SELECT p FROM Post p WHERE p.isDeleted = false AND " +
            "(p.author.id = :userId OR p.author IN (SELECT f FROM User u JOIN u.following f WHERE u.id = :userId)) " +
            "ORDER BY p.createdAt DESC")
    Page<Post> findFeedForUser(@Param("userId") UUID userId, Pageable pageable);

    // Public feed (for visitors/explore)
    @Query("SELECT p FROM Post p WHERE p.isDeleted = false AND p.visibility = :visibility ORDER BY p.createdAt DESC")
    Page<Post> findByVisibility(@Param("visibility") Visibility visibility, Pageable pageable);

    // Posts by user
    @Query("SELECT p FROM Post p WHERE p.author.id = :userId AND p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Post> findByAuthorId(@Param("userId") UUID userId, Pageable pageable);

    // Posts by type (reels)
    @Query("SELECT p FROM Post p WHERE p.postType = :postType AND p.isDeleted = false AND p.visibility = 'PUBLIC' ORDER BY p.createdAt DESC")
    Page<Post> findByPostType(@Param("postType") PostType postType, Pageable pageable);

    // Trending posts (by likes + comments)
    @Query("SELECT p FROM Post p WHERE p.isDeleted = false AND p.visibility = 'PUBLIC' ORDER BY SIZE(p.likes) DESC, SIZE(p.comments) DESC")
    Page<Post> findTrending(Pageable pageable);

    // Search posts by caption
    @Query("SELECT p FROM Post p WHERE p.isDeleted = false AND p.visibility = 'PUBLIC' AND LOWER(p.caption) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Post> searchPosts(@Param("query") String query, Pageable pageable);

    // Count user posts
    long countByAuthorIdAndIsDeletedFalse(UUID authorId);
}
