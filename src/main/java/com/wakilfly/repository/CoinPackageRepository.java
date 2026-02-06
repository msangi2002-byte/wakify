package com.wakilfly.repository;

import com.wakilfly.model.CoinPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CoinPackageRepository extends JpaRepository<CoinPackage, UUID> {

    List<CoinPackage> findByIsActiveTrueOrderBySortOrderAsc();
}
