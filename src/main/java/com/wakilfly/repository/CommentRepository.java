package com.wakilfly.repository;

import com.wakilfly.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    Page<Comment> findByPostIdAndParentIsNullAndIsDeletedFalseOrderByCreatedAtDesc(UUID postId, Pageable pageable);

    Page<Comment> findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID parentId, Pageable pageable);

    long countByPostIdAndIsDeletedFalse(UUID postId);
}
