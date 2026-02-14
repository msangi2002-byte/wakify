package com.wakilfly.repository;

import com.wakilfly.model.AgentPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentPackageRepository extends JpaRepository<AgentPackage, UUID> {

    List<AgentPackage> findByIsActiveTrueOrderBySortOrderAsc();
}
