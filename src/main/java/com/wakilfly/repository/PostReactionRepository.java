package com.wakilfly.repository;

import com.wakilfly.model.Post;
import com.wakilfly.model.PostReaction;
import com.wakilfly.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, UUID> {
    @Query("SELECT pr FROM PostReaction pr JOIN FETCH pr.user WHERE pr.post.id = :postId ORDER BY pr.createdAt ASC")
    List<PostReaction> findTopByPostIdWithUser(@Param("postId") UUID postId, Pageable pageable);
    Optional<PostReaction> findByPostAndUser(Post post, User user);

    long countByPost(Post post);

    /** Count reactions given by this user on posts by this author (for relationship strength in feed). */
    long countByUser_IdAndPost_Author_Id(UUID userId, UUID authorId);

    /** Post IDs this user reacted to (for interest vector). */
    @Query("SELECT pr.post.id FROM PostReaction pr WHERE pr.user.id = :userId")
    List<UUID> findPostIdsByUserId(@Param("userId") UUID userId);

    /** Count distinct users who have given at least one reaction – for "Engaged" segment. */
    @Query("SELECT COUNT(DISTINCT pr.user.id) FROM PostReaction pr")
    long countDistinctReactors();
}
