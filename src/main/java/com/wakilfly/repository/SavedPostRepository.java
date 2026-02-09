package com.wakilfly.repository;

import com.wakilfly.model.Post;
import com.wakilfly.model.SavedPost;
import com.wakilfly.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedPostRepository extends JpaRepository<SavedPost, UUID> {

    Page<SavedPost> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<SavedPost> findByUserAndPost(User user, Post post);

    boolean existsByUserIdAndPostId(UUID userId, UUID postId);

    void deleteByUserIdAndPostId(UUID userId, UUID postId);
}
