package com.wakilfly.repository;

import com.wakilfly.model.Post;
import com.wakilfly.model.PostReaction;
import com.wakilfly.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, UUID> {
    Optional<PostReaction> findByPostAndUser(Post post, User user);

    long countByPost(Post post);

    /** Count reactions given by this user on posts by this author (for relationship strength in feed). */
    long countByUser_IdAndPost_Author_Id(UUID userId, UUID authorId);
}
