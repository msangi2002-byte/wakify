package com.wakilfly.repository;

import com.wakilfly.model.AgentRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentRatingRepository extends JpaRepository<AgentRating, UUID> {

    Optional<AgentRating> findByRaterUserIdAndAgentId(UUID raterUserId, UUID agentId);

    @Query("SELECT COALESCE(AVG(ar.rating), 0) FROM AgentRating ar WHERE ar.agent.id = :agentId")
    Double getAverageRatingByAgentId(@Param("agentId") UUID agentId);

    @Query("SELECT COUNT(ar) FROM AgentRating ar WHERE ar.agent.id = :agentId")
    long countByAgentId(@Param("agentId") UUID agentId);

    /** Batch: agentId -> [avgRating, count] for given agent ids. */
    @Query("SELECT ar.agent.id, AVG(ar.rating), COUNT(ar) FROM AgentRating ar WHERE ar.agent.id IN :agentIds GROUP BY ar.agent.id")
    List<Object[]> getAverageAndCountByAgentIds(@Param("agentIds") List<UUID> agentIds);
}
