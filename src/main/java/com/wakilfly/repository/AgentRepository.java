package com.wakilfly.repository;

import com.wakilfly.model.Agent;
import com.wakilfly.model.AgentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentRepository extends JpaRepository<Agent, UUID> {

        Optional<Agent> findByUserId(UUID userId);

        Optional<Agent> findByAgentCode(String agentCode);

        boolean existsByUserId(UUID userId);

        boolean existsByAgentCode(String agentCode);

        boolean existsByNationalId(String nationalId);

        Page<Agent> findByStatus(AgentStatus status, Pageable pageable);

        @Query("SELECT a FROM Agent a WHERE a.region = :region AND a.status = 'ACTIVE'")
        Page<Agent> findActiveAgentsByRegion(@Param("region") String region, Pageable pageable);

        @Query("SELECT a FROM Agent a WHERE a.region = :region AND a.district = :district AND a.status = 'ACTIVE'")
        Page<Agent> findActiveAgentsByDistrict(@Param("region") String region, @Param("district") String district,
                        Pageable pageable);

        @Query("SELECT a FROM Agent a WHERE " +
                        "(LOWER(a.user.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "LOWER(COALESCE(a.user.email, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                        "a.agentCode LIKE CONCAT('%', :query, '%') OR " +
                        "a.user.phone LIKE CONCAT('%', :query, '%')) AND (a.status = 'ACTIVE' OR a.status = 'PENDING')")
        Page<Agent> searchAgents(@Param("query") String query, Pageable pageable);

        @Query("SELECT COUNT(a) FROM Agent a WHERE a.status = 'ACTIVE'")
        long countActiveAgents();

        long countByStatus(AgentStatus status);
}
