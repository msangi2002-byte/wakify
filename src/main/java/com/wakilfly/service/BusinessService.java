package com.wakilfly.service;

import com.wakilfly.dto.response.BusinessResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.PostResponse;
import com.wakilfly.entity.Business;
import com.wakilfly.entity.BusinessStatus;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService {

    private final BusinessRepository businessRepository;

    /**
     * Get business by ID
     */
    public BusinessResponse getBusinessById(UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));
        return mapToBusinessResponse(business);
    }

    /**
     * Get business by owner ID
     */
    public BusinessResponse getBusinessByOwnerId(UUID ownerId) {
        Business business = businessRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found for this user"));
        return mapToBusinessResponse(business);
    }

    /**
     * Search businesses
     */
    public PagedResponse<BusinessResponse> searchBusinesses(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Business> businesses = businessRepository.searchBusinesses(query, pageable);

        return buildPagedResponse(businesses);
    }

    /**
     * Get businesses by category
     */
    public PagedResponse<BusinessResponse> getBusinessesByCategory(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Business> businesses = businessRepository.findActiveByCategory(category, pageable);

        return buildPagedResponse(businesses);
    }

    /**
     * Get businesses by region
     */
    public PagedResponse<BusinessResponse> getBusinessesByRegion(String region, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Business> businesses = businessRepository.findActiveByRegion(region, pageable);

        return buildPagedResponse(businesses);
    }

    /**
     * Get all active businesses
     */
    public PagedResponse<BusinessResponse> getAllBusinesses(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Business> businesses = businessRepository.findByStatus(BusinessStatus.ACTIVE, pageable);

        return buildPagedResponse(businesses);
    }

    // Helper methods

    private PagedResponse<BusinessResponse> buildPagedResponse(Page<Business> businesses) {
        return PagedResponse.<BusinessResponse>builder()
                .content(businesses.getContent().stream()
                        .map(this::mapToBusinessResponse)
                        .collect(Collectors.toList()))
                .page(businesses.getNumber())
                .size(businesses.getSize())
                .totalElements(businesses.getTotalElements())
                .totalPages(businesses.getTotalPages())
                .last(businesses.isLast())
                .first(businesses.isFirst())
                .build();
    }

    private BusinessResponse mapToBusinessResponse(Business business) {
        return BusinessResponse.builder()
                .id(business.getId())
                .name(business.getName())
                .description(business.getDescription())
                .category(business.getCategory())
                .logo(business.getLogo())
                .coverImage(business.getCoverImage())
                .status(business.getStatus())
                .isVerified(business.getIsVerified())
                .region(business.getRegion())
                .district(business.getDistrict())
                .ward(business.getWard())
                .street(business.getStreet())
                .latitude(business.getLatitude())
                .longitude(business.getLongitude())
                .owner(PostResponse.UserSummary.builder()
                        .id(business.getOwner().getId())
                        .name(business.getOwner().getName())
                        .profilePic(business.getOwner().getProfilePic())
                        .build())
                .agentId(business.getAgent() != null ? business.getAgent().getId() : null)
                .agentName(business.getAgent() != null ? business.getAgent().getUser().getName() : null)
                .agentCode(business.getAgent() != null ? business.getAgent().getAgentCode() : null)
                .productsCount(business.getProductsCount())
                .rating(business.getRating())
                .reviewsCount(business.getReviewsCount())
                .createdAt(business.getCreatedAt())
                .build();
    }
}
