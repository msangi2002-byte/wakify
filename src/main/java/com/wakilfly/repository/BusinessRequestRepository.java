package com.wakilfly.repository;

import com.wakilfly.model.BusinessRequest;
import com.wakilfly.model.BusinessRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BusinessRequestRepository extends JpaRepository<BusinessRequest, UUID> {

    @Query("SELECT br FROM BusinessRequest br WHERE br.agent.id = :agentId ORDER BY br.createdAt DESC")
    Page<BusinessRequest> findByAgentIdOrderByCreatedAtDesc(@Param("agentId") UUID agentId, Pageable pageable);

    boolean existsByUserIdAndStatus(UUID userId, BusinessRequestStatus status);

    @Query("SELECT br FROM BusinessRequest br WHERE br.user.id = :userId ORDER BY br.createdAt DESC")
    Page<BusinessRequest> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);
}
