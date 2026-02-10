package com.wakilfly.repository;

import com.wakilfly.model.Comment;
import com.wakilfly.model.CommentLike;
import com.wakilfly.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

    Optional<CommentLike> findByCommentAndUser(Comment comment, User user);

    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

    long countByComment(Comment comment);
}
