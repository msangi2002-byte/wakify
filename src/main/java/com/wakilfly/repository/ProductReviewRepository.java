package com.wakilfly.repository;

import com.wakilfly.model.Product;
import com.wakilfly.model.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {
    Page<ProductReview> findByProductId(UUID productId, Pageable pageable);

    // For aggregates
    long countByProduct(Product product);
}
