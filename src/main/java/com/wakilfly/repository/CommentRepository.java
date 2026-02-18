package com.wakilfly.repository;

import com.wakilfly.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    Page<Comment> findByPostIdAndParentIsNullAndIsDeletedFalseOrderByCreatedAtDesc(UUID postId, Pageable pageable);

    Page<Comment> findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID parentId, Pageable pageable);

    long countByPostIdAndIsDeletedFalse(UUID postId);

    /** Count comments by this user on posts by this author (for relationship strength in feed). */
    long countByAuthor_IdAndPost_Author_Id(UUID commentAuthorId, UUID postAuthorId);

    /** Count distinct users who have commented (non-deleted) – for "Engaged" segment. */
    @Query("SELECT COUNT(DISTINCT c.author.id) FROM Comment c WHERE c.isDeleted = false")
    long countDistinctCommenters();
}
