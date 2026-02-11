package com.wakilfly.repository;

import com.wakilfly.model.BusinessFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessFollowRepository extends JpaRepository<BusinessFollow, UUID> {

    Optional<BusinessFollow> findByUserIdAndBusinessId(UUID userId, UUID businessId);

    boolean existsByUserIdAndBusinessId(UUID userId, UUID businessId);

    void deleteByUserIdAndBusinessId(UUID userId, UUID businessId);

    long countByBusinessId(UUID businessId);

    Page<BusinessFollow> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
