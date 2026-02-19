package com.wakilfly.repository;

import com.wakilfly.model.Business;
import com.wakilfly.model.BusinessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Optional<Business> findByOwnerId(UUID ownerId);

    Page<Business> findByAgentId(UUID agentId, Pageable pageable);

    Page<Business> findByStatus(BusinessStatus status, Pageable pageable);

    @Query("SELECT b FROM Business b WHERE b.region = :region AND b.status = 'ACTIVE'")
    Page<Business> findActiveByRegion(@Param("region") String region, Pageable pageable);

    @Query("SELECT b FROM Business b WHERE b.category = :category AND b.status = 'ACTIVE'")
    Page<Business> findActiveByCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT b FROM Business b WHERE " +
            "(LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(b.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(b.category) LIKE LOWER(CONCAT('%', :query, '%'))) AND b.status = 'ACTIVE'")
    Page<Business> searchBusinesses(@Param("query") String query, Pageable pageable);

    /** Used by product search: get business IDs whose name/description/category match the query (max 500). */
    @Query("SELECT b.id FROM Business b WHERE " +
            "(LOWER(b.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(b.description, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(b.category, '')) LIKE LOWER(CONCAT('%', :q, '%'))) AND b.status = 'ACTIVE'")
    List<UUID> findActiveBusinessIdsBySearch(@Param("q") String q, Pageable pageable);

    @Query("SELECT COUNT(b) FROM Business b WHERE b.agent.id = :agentId")
    long countByAgentId(@Param("agentId") UUID agentId);

    @Query("SELECT COUNT(b) FROM Business b WHERE b.agent.id = :agentId AND b.status = 'ACTIVE'")
    long countActiveByAgentId(@Param("agentId") UUID agentId);

    long countByStatus(BusinessStatus status);

    @Query("SELECT b FROM Business b WHERE b.latitude IS NOT NULL AND b.longitude IS NOT NULL")
    List<Business> findAllWithCoordinates();
}
