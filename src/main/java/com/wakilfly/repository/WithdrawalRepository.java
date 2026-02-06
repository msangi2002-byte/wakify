package com.wakilfly.repository;

import com.wakilfly.model.Withdrawal;
import com.wakilfly.model.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, UUID> {

    Page<Withdrawal> findByAgentIdOrderByCreatedAtDesc(UUID agentId, Pageable pageable);

    Page<Withdrawal> findByStatusOrderByCreatedAtDesc(WithdrawalStatus status, Pageable pageable);

    Page<Withdrawal> findByAgentIdAndStatusOrderByCreatedAtDesc(UUID agentId, WithdrawalStatus status,
            Pageable pageable);

    List<Withdrawal> findByStatus(WithdrawalStatus status);

    @Query("SELECT SUM(w.amount) FROM Withdrawal w WHERE w.agent.id = :agentId AND w.status = :status")
    BigDecimal sumAmountByAgentIdAndStatus(@Param("agentId") UUID agentId, @Param("status") WithdrawalStatus status);

    @Query("SELECT SUM(w.amount) FROM Withdrawal w WHERE w.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") WithdrawalStatus status);

    boolean existsByAgentIdAndStatus(UUID agentId, WithdrawalStatus status);
}
