package com.wakilfly.repository;

import com.wakilfly.model.VirtualGift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VirtualGiftRepository extends JpaRepository<VirtualGift, UUID> {

    List<VirtualGift> findByIsActiveTrueOrderBySortOrderAsc();

    List<VirtualGift> findByIsPremiumAndIsActiveTrueOrderBySortOrderAsc(Boolean isPremium);

    Optional<VirtualGift> findByName(String name);
}
