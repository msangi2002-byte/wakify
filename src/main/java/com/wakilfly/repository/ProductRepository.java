package com.wakilfly.repository;

import com.wakilfly.entity.Product;
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

    @Query("SELECT p FROM Product p WHERE " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%'))) AND p.isActive = true")
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.business.region = :region AND p.isActive = true")
    Page<Product> findByRegion(@Param("region") String region, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.viewsCount DESC")
    Page<Product> findTrending(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.business.id = :businessId")
    long countByBusinessId(@Param("businessId") UUID businessId);
}
