package com.wakilfly.repository;

import com.wakilfly.model.Ad;
import com.wakilfly.model.AdStatus;
import com.wakilfly.model.AdType;
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
public interface AdRepository extends JpaRepository<Ad, UUID> {

    Page<Ad> findByBusinessIdOrderByCreatedAtDesc(UUID businessId, Pageable pageable);

    Page<Ad> findByStatusOrderByCreatedAtDesc(AdStatus status, Pageable pageable);

    @Query("SELECT a FROM Ad a WHERE a.status = 'ACTIVE' AND a.type = :type " +
            "AND (a.startDate IS NULL OR a.startDate <= :now) " +
            "AND (a.endDate IS NULL OR a.endDate >= :now) " +
            "ORDER BY RANDOM()")
    List<Ad> findActiveAdsByType(@Param("type") AdType type, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT a FROM Ad a WHERE a.status = 'ACTIVE' " +
            "AND (a.startDate IS NULL OR a.startDate <= :now) " +
            "AND (a.endDate IS NULL OR a.endDate >= :now) " +
            "AND (a.targetRegions IS NULL OR a.targetRegions LIKE %:region%) " +
            "ORDER BY RANDOM()")
    List<Ad> findActiveAdsForRegion(@Param("region") String region, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Ad a WHERE a.business.id = :businessId")
    long countByBusinessId(@Param("businessId") UUID businessId);
}
