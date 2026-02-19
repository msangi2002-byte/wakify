package com.wakilfly.repository;

import com.wakilfly.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findByBusinessIdAndIsActiveTrue(UUID businessId, Pageable pageable);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.isActive = true")
    Page<Product> findByCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN p.business b WHERE " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%'))) AND p.isActive = true")
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.business.region = :region AND p.isActive = true")
    Page<Product> findByRegion(@Param("region") String region, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.viewsCount DESC")
    Page<Product> findTrending(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.business.id = :businessId")
    long countByBusinessId(@Param("businessId") UUID businessId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.business.id = :businessId AND p.isActive = true")
    long countActiveByBusinessId(@Param("businessId") UUID businessId);

    long countByIsActiveTrue();

    @Query("SELECT p FROM Product p WHERE (:businessId IS NULL OR p.business.id = :businessId) AND (:active IS NULL OR p.isActive = :active) ORDER BY p.createdAt DESC")
    Page<Product> findAllForAdmin(@Param("businessId") UUID businessId, @Param("active") Boolean active, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE (:businessId IS NULL OR p.business.id = :businessId) AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.category) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY p.createdAt DESC")
    Page<Product> searchForAdmin(@Param("businessId") UUID businessId, @Param("q") String q, Pageable pageable);
}
