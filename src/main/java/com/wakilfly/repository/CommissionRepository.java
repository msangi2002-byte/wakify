package com.wakilfly.repository;

import com.wakilfly.model.Commission;
import com.wakilfly.model.CommissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, UUID> {

    Page<Commission> findByAgentIdOrderByCreatedAtDesc(UUID agentId, Pageable pageable);

    Page<Commission> findByAgentIdAndStatus(UUID agentId, CommissionStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Commission c WHERE c.agent.id = :agentId")
    BigDecimal sumTotalByAgentId(@Param("agentId") UUID agentId);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Commission c WHERE c.agent.id = :agentId AND c.status = 'PENDING'")
    BigDecimal sumPendingByAgentId(@Param("agentId") UUID agentId);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Commission c WHERE c.agent.id = :agentId AND c.status = 'PAID'")
    BigDecimal sumPaidByAgentId(@Param("agentId") UUID agentId);
}
