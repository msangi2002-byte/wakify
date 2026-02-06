package com.wakilfly.repository;

import com.wakilfly.model.Promotion;
import com.wakilfly.model.PromotionStatus;
import com.wakilfly.model.PromotionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    // Get user's promotions
    Page<Promotion> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Get business promotions
    Page<Promotion> findByBusinessIdOrderByCreatedAtDesc(UUID businessId, Pageable pageable);

    // Get promotions by status
    Page<Promotion> findByStatusOrderByCreatedAtDesc(PromotionStatus status, Pageable pageable);

    // Get active promotions
    @Query("SELECT p FROM Promotion p WHERE p.status = 'ACTIVE' AND p.startDate <= :now AND p.endDate >= :now AND p.spentAmount < p.budget")
    List<Promotion> findActivePromotions(@Param("now") LocalDateTime now);

    // Get promotions by type
    Page<Promotion> findByTypeAndStatusOrderByCreatedAtDesc(PromotionType type, PromotionStatus status,
            Pageable pageable);

    // Get active promotion for a specific target
    @Query("SELECT p FROM Promotion p WHERE p.targetId = :targetId AND p.type = :type AND p.status = 'ACTIVE' AND p.startDate <= :now AND p.endDate >= :now")
    List<Promotion> findActivePromotionsByTarget(@Param("targetId") UUID targetId, @Param("type") PromotionType type,
            @Param("now") LocalDateTime now);

    // Count active promotions
    long countByStatus(PromotionStatus status);

    // Get promotions to end (past end date but still active)
    @Query("SELECT p FROM Promotion p WHERE p.status = 'ACTIVE' AND p.endDate < :now")
    List<Promotion> findPromotionsToEnd(@Param("now") LocalDateTime now);

    // Get promotions to start (past start date but still pending)
    @Query("SELECT p FROM Promotion p WHERE p.status = 'PENDING' AND p.isPaid = true AND p.startDate <= :now")
    List<Promotion> findPromotionsToStart(@Param("now") LocalDateTime now);
}
