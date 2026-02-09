package com.wakilfly.repository;

import com.wakilfly.model.Hashtag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, UUID> {

    Optional<Hashtag> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    /** Posts by hashtag name (for Explore / search by #tag) */
    @Query("SELECT p FROM Post p JOIN p.hashtags h WHERE LOWER(h.name) = LOWER(:tagName) AND p.isDeleted = false AND p.visibility = 'PUBLIC' ORDER BY p.createdAt DESC")
    Page<com.wakilfly.model.Post> findPostsByHashtagName(@Param("tagName") String tagName, Pageable pageable);

    /** Trending: hashtags with most posts (for Explore) */
    @Query("SELECT h FROM Hashtag h WHERE SIZE(h.posts) > 0 ORDER BY SIZE(h.posts) DESC")
    Page<Hashtag> findTrendingHashtags(Pageable pageable);
}
