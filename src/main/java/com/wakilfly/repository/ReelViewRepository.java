package com.wakilfly.repository;

import com.wakilfly.model.ReelView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReelViewRepository extends JpaRepository<ReelView, UUID> {

    /**
     * Aggregate watch time and completion rate per post for scoring.
     * Returns: postId, avgWatchTimeSeconds (Double), completionRate (Double 0-1).
     */
    @Query(value = "SELECT rv.post_id, " +
            "AVG(rv.watch_time_seconds), " +
            "COALESCE(SUM(CASE WHEN rv.completed = 1 THEN 1 ELSE 0 END) * 1.0 / NULLIF(COUNT(*), 0), 0) " +
            "FROM reel_views rv WHERE rv.post_id IN :postIds GROUP BY rv.post_id", nativeQuery = true)
    List<Object[]> getReelStatsByPostIds(@Param("postIds") List<UUID> postIds);

    /** Post IDs that this user completed (for interest vector). */
    @Query("SELECT rv.post.id FROM ReelView rv WHERE rv.user.id = :userId AND rv.completed = true")
    List<UUID> findPostIdsByUserIdAndCompletedTrue(@Param("userId") UUID userId);
}
