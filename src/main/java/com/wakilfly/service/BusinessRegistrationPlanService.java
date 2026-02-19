package com.wakilfly.service;

import com.wakilfly.dto.request.BusinessRegistrationPlanRequest;
import com.wakilfly.dto.response.BusinessRegistrationPlanResponse;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.BusinessRegistrationPlan;
import com.wakilfly.repository.BusinessRegistrationPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessRegistrationPlanService {

    private final BusinessRegistrationPlanRepository repository;

    public List<BusinessRegistrationPlanResponse> getActivePlans() {
        return repository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<BusinessRegistrationPlanResponse> getAllPlans() {
        return repository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public BusinessRegistrationPlanResponse getById(UUID id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business registration plan", "id", id)));
    }

    @Transactional
    public BusinessRegistrationPlanResponse create(BusinessRegistrationPlanRequest request) {
        BusinessRegistrationPlan plan = BusinessRegistrationPlan.builder()
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .price(request.getPrice())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        plan = repository.save(plan);
        return toResponse(plan);
    }

    @Transactional
    public BusinessRegistrationPlanResponse update(UUID id, BusinessRegistrationPlanRequest request) {
        BusinessRegistrationPlan plan = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business registration plan", "id", id));
        plan.setName(request.getName().trim());
        plan.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        plan.setPrice(request.getPrice());
        if (request.getSortOrder() != null) plan.setSortOrder(request.getSortOrder());
        if (request.getIsActive() != null) plan.setIsActive(request.getIsActive());
        plan = repository.save(plan);
        return toResponse(plan);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Business registration plan", "id", id);
        }
        repository.deleteById(id);
    }

    public java.util.Optional<BusinessRegistrationPlan> findById(UUID id) {
        return repository.findById(id);
    }

    private BusinessRegistrationPlanResponse toResponse(BusinessRegistrationPlan p) {
        return BusinessRegistrationPlanResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .sortOrder(p.getSortOrder())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
