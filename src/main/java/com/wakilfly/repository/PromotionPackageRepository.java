package com.wakilfly.repository;

import com.wakilfly.model.PromotionPackage;
import com.wakilfly.model.PromotionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PromotionPackageRepository extends JpaRepository<PromotionPackage, UUID> {

    List<PromotionPackage> findByIsActiveTrueOrderBySortOrderAsc();

    List<PromotionPackage> findByPromotionTypeAndIsActiveTrueOrderBySortOrderAsc(PromotionType type);
}
