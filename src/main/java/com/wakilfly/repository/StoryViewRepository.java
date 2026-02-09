package com.wakilfly.repository;

import com.wakilfly.model.Post;
import com.wakilfly.model.StoryView;
import com.wakilfly.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoryViewRepository extends JpaRepository<StoryView, UUID> {

    Page<StoryView> findByPostIdOrderByViewedAtDesc(UUID postId, Pageable pageable);

    Optional<StoryView> findByViewerAndPost(User viewer, Post post);

    boolean existsByViewerIdAndPostId(UUID viewerId, UUID postId);
}
