package com.wakilfly.repository;

import com.wakilfly.model.BusinessRegistrationPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BusinessRegistrationPlanRepository extends JpaRepository<BusinessRegistrationPlan, UUID> {

    List<BusinessRegistrationPlan> findByIsActiveTrueOrderBySortOrderAsc();
    List<BusinessRegistrationPlan> findAllByOrderBySortOrderAsc();
}
