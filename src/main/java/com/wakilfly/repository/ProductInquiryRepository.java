package com.wakilfly.repository;

import com.wakilfly.model.InquiryStatus;
import com.wakilfly.model.ProductInquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductInquiryRepository extends JpaRepository<ProductInquiry, UUID> {

    Page<ProductInquiry> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId, Pageable pageable);

    Page<ProductInquiry> findByBusinessIdOrderByCreatedAtDesc(UUID businessId, Pageable pageable);

    Page<ProductInquiry> findByBusinessIdAndStatusOrderByCreatedAtDesc(UUID businessId, InquiryStatus status, Pageable pageable);

    @Query("SELECT i FROM ProductInquiry i WHERE i.business.id = :businessId ORDER BY i.createdAt DESC")
    Page<ProductInquiry> findByBusinessId(@Param("businessId") UUID businessId, Pageable pageable);

    long countByBusinessIdAndStatus(UUID businessId, InquiryStatus status);
}
