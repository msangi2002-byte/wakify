package com.wakilfly.service;

import com.wakilfly.dto.request.CreateAdRequest;
import com.wakilfly.dto.response.AdResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.*;
import com.wakilfly.repository.AdRepository;
import com.wakilfly.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdService {

    private final AdRepository adRepository;
    private final BusinessRepository businessRepository;

    /**
     * Create a new ad
     */
    @Transactional
    public AdResponse createAd(UUID businessOwnerId, CreateAdRequest request) {
        Business business = businessRepository.findByOwnerId(businessOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        Ad ad = Ad.builder()
                .business(business)
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .videoUrl(request.getVideoUrl())
                .targetUrl(request.getTargetUrl())
                .type(request.getType())
                .status(AdStatus.PENDING)
                .dailyBudget(request.getDailyBudget())
                .totalBudget(request.getTotalBudget())
                .targetRegions(request.getTargetRegions())
                .targetCategories(request.getTargetCategories())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        ad = adRepository.save(ad);
        return mapToAdResponse(ad);
    }

    /**
     * Get my ads (for business owner)
     */
    public PagedResponse<AdResponse> getMyAds(UUID businessOwnerId, int page, int size) {
        Business business = businessRepository.findByOwnerId(businessOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Ad> ads = adRepository.findByBusinessIdOrderByCreatedAtDesc(business.getId(), pageable);

        return PagedResponse.<AdResponse>builder()
                .content(ads.getContent().stream()
                        .map(this::mapToAdResponse)
                        .collect(Collectors.toList()))
                .page(ads.getNumber())
                .size(ads.getSize())
                .totalElements(ads.getTotalElements())
                .totalPages(ads.getTotalPages())
                .last(ads.isLast())
                .first(ads.isFirst())
                .build();
    }

    /**
     * Get active ads for display (for users)
     */
    public List<AdResponse> getActiveAds(AdType type, String region, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        LocalDateTime now = LocalDateTime.now();

        List<Ad> ads;
        if (region != null && !region.isEmpty()) {
            ads = adRepository.findActiveAdsForRegion(region, now, pageable);
        } else {
            ads = adRepository.findActiveAdsByType(type, now, pageable);
        }

        return ads.stream().map(this::mapToAdResponse).collect(Collectors.toList());
    }

    /**
     * Record ad impression
     */
    @Transactional
    public void recordImpression(UUID adId) {
        Ad ad = adRepository.findById(adId).orElse(null);
        if (ad != null) {
            ad.setImpressions(ad.getImpressions() + 1);
            adRepository.save(ad);
        }
    }

    /**
     * Record ad click
     */
    @Transactional
    public void recordClick(UUID adId) {
        Ad ad = adRepository.findById(adId).orElse(null);
        if (ad != null) {
            ad.setClicks(ad.getClicks() + 1);
            // Deduct CPC from budget
            if (ad.getCostPerClick() != null) {
                ad.setAmountSpent(ad.getAmountSpent().add(ad.getCostPerClick()));
            }
            adRepository.save(ad);
        }
    }

    /**
     * Pause an ad
     */
    @Transactional
    public AdResponse pauseAd(UUID adId, UUID businessOwnerId) {
        Ad ad = getAdByIdAndOwner(adId, businessOwnerId);
        ad.setStatus(AdStatus.PAUSED);
        ad = adRepository.save(ad);
        return mapToAdResponse(ad);
    }

    /**
     * Resume an ad
     */
    @Transactional
    public AdResponse resumeAd(UUID adId, UUID businessOwnerId) {
        Ad ad = getAdByIdAndOwner(adId, businessOwnerId);
        ad.setStatus(AdStatus.ACTIVE);
        ad = adRepository.save(ad);
        return mapToAdResponse(ad);
    }

    private Ad getAdByIdAndOwner(UUID adId, UUID businessOwnerId) {
        Business business = businessRepository.findByOwnerId(businessOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new ResourceNotFoundException("Ad", "id", adId));

        if (!ad.getBusiness().getId().equals(business.getId())) {
            throw new RuntimeException("Not authorized to modify this ad");
        }

        return ad;
    }

    private AdResponse mapToAdResponse(Ad ad) {
        double ctr = ad.getImpressions() > 0
                ? (double) ad.getClicks() / ad.getImpressions() * 100
                : 0.0;

        return AdResponse.builder()
                .id(ad.getId())
                .title(ad.getTitle())
                .description(ad.getDescription())
                .imageUrl(ad.getImageUrl())
                .videoUrl(ad.getVideoUrl())
                .targetUrl(ad.getTargetUrl())
                .type(ad.getType())
                .status(ad.getStatus())
                .dailyBudget(ad.getDailyBudget())
                .totalBudget(ad.getTotalBudget())
                .targetRegions(ad.getTargetRegions())
                .targetCategories(ad.getTargetCategories())
                .startDate(ad.getStartDate())
                .endDate(ad.getEndDate())
                .impressions(ad.getImpressions())
                .clicks(ad.getClicks())
                .amountSpent(ad.getAmountSpent())
                .ctr(ctr)
                .business(AdResponse.BusinessSummary.builder()
                        .id(ad.getBusiness().getId())
                        .name(ad.getBusiness().getName())
                        .logo(ad.getBusiness().getLogo())
                        .build())
                .createdAt(ad.getCreatedAt())
                .build();
    }
}
