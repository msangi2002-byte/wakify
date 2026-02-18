package com.wakilfly.service;

import com.wakilfly.dto.request.CreatePromotionRequest;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.PromotionPackageResponse;
import com.wakilfly.dto.response.PromotionResponse;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.*;
import com.wakilfly.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionPackageRepository packageRepository;
    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final PostRepository postRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Create a new promotion
     */
    @Transactional
    public PromotionResponse createPromotion(UUID userId, CreatePromotionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Validate target exists
        validateTarget(request.getType(), request.getTargetId(), userId);

        // Validate dates
        if (request.getStartDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Start date cannot be in the past");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

        // Get business if user has one
        Business business = businessRepository.findByOwnerId(userId).orElse(null);

        Promotion promotion = Promotion.builder()
                .user(user)
                .business(business)
                .type(request.getType())
                .targetId(request.getTargetId())
                .title(request.getTitle())
                .description(request.getDescription())
                .budget(request.getBudget())
                .dailyBudget(request.getDailyBudget())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .targetRegions(request.getTargetRegions() != null ? String.join(",", request.getTargetRegions()) : null)
                .targetAgeMin(request.getTargetAgeMin())
                .targetAgeMax(request.getTargetAgeMax())
                .targetGender(request.getTargetGender())
                .status(PromotionStatus.PENDING)
                .build();

        promotion = promotionRepository.save(promotion);

        // Create payment record
        Payment payment = Payment.builder()
                .user(user)
                .amount(request.getBudget())
                .type(PaymentType.PROMOTION)
                .status(PaymentStatus.PENDING)
                .description("Promotion: " + request.getTitle())
                .transactionId("PROMO" + System.currentTimeMillis())
                .paymentPhone(request.getPaymentPhone())
                .build();

        payment = paymentRepository.save(payment);
        promotion.setPaymentId(payment.getId());
        promotionRepository.save(promotion);

        log.info("Promotion {} created by user {}. Awaiting payment.", promotion.getId(), userId);

        return mapToResponse(promotion);
    }

    /**
     * Get user's promotions
     */
    public PagedResponse<PromotionResponse> getUserPromotions(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Promotion> promotions = promotionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return PagedResponse.<PromotionResponse>builder()
                .content(promotions.getContent().stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()))
                .page(promotions.getNumber())
                .size(promotions.getSize())
                .totalElements(promotions.getTotalElements())
                .totalPages(promotions.getTotalPages())
                .last(promotions.isLast())
                .first(promotions.isFirst())
                .build();
    }

    /**
     * Get promotion by ID
     */
    public PromotionResponse getPromotion(UUID promotionId, UUID userId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", promotionId));

        if (!promotion.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Promotion", "id", promotionId);
        }

        return mapToResponse(promotion);
    }

    /**
     * Pause a promotion
     */
    @Transactional
    public PromotionResponse pausePromotion(UUID promotionId, UUID userId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", promotionId));

        if (!promotion.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Promotion", "id", promotionId);
        }

        if (promotion.getStatus() != PromotionStatus.ACTIVE) {
            throw new BadRequestException("Only active promotions can be paused");
        }

        promotion.setStatus(PromotionStatus.PAUSED);
        promotion = promotionRepository.save(promotion);

        log.info("Promotion {} paused by user {}", promotionId, userId);

        return mapToResponse(promotion);
    }

    /**
     * Resume a paused promotion
     */
    @Transactional
    public PromotionResponse resumePromotion(UUID promotionId, UUID userId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", promotionId));

        if (!promotion.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Promotion", "id", promotionId);
        }

        if (promotion.getStatus() != PromotionStatus.PAUSED) {
            throw new BadRequestException("Only paused promotions can be resumed");
        }

        if (promotion.getEndDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot resume expired promotion");
        }

        promotion.setStatus(PromotionStatus.ACTIVE);
        promotion = promotionRepository.save(promotion);

        log.info("Promotion {} resumed by user {}", promotionId, userId);

        return mapToResponse(promotion);
    }

    /**
     * Cancel a promotion
     */
    @Transactional
    public void cancelPromotion(UUID promotionId, UUID userId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", promotionId));

        if (!promotion.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Promotion", "id", promotionId);
        }

        if (promotion.getStatus() == PromotionStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed promotion");
        }

        promotion.setStatus(PromotionStatus.CANCELLED);
        promotionRepository.save(promotion);

        // TODO: Process refund for remaining budget

        log.info("Promotion {} cancelled by user {}", promotionId, userId);
    }

    /**
     * Get promotion stats
     */
    public PromotionResponse.PromotionStats getPromotionStats(UUID promotionId, UUID userId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", promotionId));

        if (!promotion.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Promotion", "id", promotionId);
        }

        return PromotionResponse.PromotionStats.builder()
                .totalImpressions(promotion.getImpressions())
                .totalClicks(promotion.getClicks())
                .totalConversions(promotion.getConversions())
                .totalSpent(promotion.getSpentAmount())
                .averageCtr(promotion.getCtr())
                .build();
    }

    /**
     * Track impression
     */
    @Transactional
    public void trackImpression(UUID promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId).orElse(null);
        if (promotion != null && promotion.getStatus() == PromotionStatus.ACTIVE) {
            promotion.incrementImpressions();
            promotionRepository.save(promotion);
        }
    }

    /**
     * Track click
     */
    @Transactional
    public void trackClick(UUID promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId).orElse(null);
        if (promotion != null && promotion.getStatus() == PromotionStatus.ACTIVE) {
            promotion.incrementClicks();
            promotionRepository.save(promotion);
        }
    }

    /**
     * Get promotion packages
     */
    public List<PromotionPackageResponse> getPackages(PromotionType type) {
        List<PromotionPackage> packages;
        if (type != null) {
            packages = packageRepository.findByPromotionTypeAndIsActiveTrueOrderBySortOrderAsc(type);
        } else {
            packages = packageRepository.findByIsActiveTrueOrderBySortOrderAsc();
        }

        return packages.stream()
                .map(this::mapPackageToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Confirm promotion payment
     */
    @Transactional
    public void confirmPayment(UUID promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", promotionId));

        promotion.setIsPaid(true);

        // If start date is now or past, activate immediately
        if (!promotion.getStartDate().isAfter(LocalDateTime.now())) {
            promotion.setStatus(PromotionStatus.ACTIVE);
        }

        promotionRepository.save(promotion);
        log.info("Promotion {} payment confirmed", promotionId);
    }

    /**
     * Scheduled job to start/end promotions
     */
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void processPromotions() {
        LocalDateTime now = LocalDateTime.now();

        // Start pending promotions
        List<Promotion> toStart = promotionRepository.findPromotionsToStart(now);
        for (Promotion promotion : toStart) {
            promotion.setStatus(PromotionStatus.ACTIVE);
            promotionRepository.save(promotion);
            log.info("Promotion {} started", promotion.getId());
        }

        // End expired promotions
        List<Promotion> toEnd = promotionRepository.findPromotionsToEnd(now);
        for (Promotion promotion : toEnd) {
            promotion.setStatus(PromotionStatus.COMPLETED);
            promotionRepository.save(promotion);
            log.info("Promotion {} completed", promotion.getId());
        }
    }

    // Helper methods

    private void validateTarget(PromotionType type, UUID targetId, UUID userId) {
        switch (type) {
            case POST -> {
                Post post = postRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Post", "id", targetId));
                if (!post.getAuthor().getId().equals(userId)) {
                    throw new BadRequestException("You can only promote your own posts");
                }
            }
            case PRODUCT -> {
                Product product = productRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Product", "id", targetId));
                if (!product.getBusiness().getOwner().getId().equals(userId)) {
                    throw new BadRequestException("You can only promote your own products");
                }
            }
            case BUSINESS -> {
                Business business = businessRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Business", "id", targetId));
                if (!business.getOwner().getId().equals(userId)) {
                    throw new BadRequestException("You can only promote your own business");
                }
            }
        }
    }

    public PromotionResponse mapToResponse(Promotion promotion) {
        return mapToResponse(promotion, false);
    }

    public PromotionResponse mapToAdminResponse(Promotion promotion) {
        return mapToResponse(promotion, true);
    }

    private PromotionResponse mapToResponse(Promotion promotion, boolean includeAdminFields) {
        int learningPhase = promotion.getLearningPhaseConversions() != null ? promotion.getLearningPhaseConversions() : 0;
        boolean inLearning = promotion.getStatus() == PromotionStatus.ACTIVE && learningPhase < 50;
        return PromotionResponse.builder()
                .id(promotion.getId())
                .type(promotion.getType())
                .status(promotion.getStatus())
                .objective(promotion.getObjective())
                .targetId(promotion.getTargetId())
                .title(promotion.getTitle())
                .description(promotion.getDescription())
                .budget(promotion.getBudget())
                .spentAmount(promotion.getSpentAmount())
                .remainingBudget(promotion.getRemainingBudget())
                .dailyBudget(promotion.getDailyBudget())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .targetRegions(
                        promotion.getTargetRegions() != null ? Arrays.asList(promotion.getTargetRegions().split(","))
                                : null)
                .targetAgeMin(promotion.getTargetAgeMin())
                .targetAgeMax(promotion.getTargetAgeMax())
                .targetGender(promotion.getTargetGender())
                .targetCountry(promotion.getTargetCountry())
                .targetCity(promotion.getTargetCity())
                .targetRadiusKm(promotion.getTargetRadiusKm())
                .targetInterests(promotion.getTargetInterests() != null && !promotion.getTargetInterests().isBlank()
                        ? Arrays.asList(promotion.getTargetInterests().split(",\\s*")) : null)
                .targetBehaviors(promotion.getTargetBehaviors() != null && !promotion.getTargetBehaviors().isBlank()
                        ? Arrays.asList(promotion.getTargetBehaviors().split(",\\s*")) : null)
                .adQualityScore(promotion.getAdQualityScore())
                .learningPhaseConversions(learningPhase)
                .isInLearningPhase(inLearning)
                .impressions(promotion.getImpressions())
                .clicks(promotion.getClicks())
                .conversions(promotion.getConversions())
                .reach(promotion.getReach())
                .ctr(promotion.getCtr())
                .costPerClick(promotion.getCostPerClick())
                .isPaid(promotion.getIsPaid())
                .userId(includeAdminFields && promotion.getUser() != null ? promotion.getUser().getId() : null)
                .userName(includeAdminFields && promotion.getUser() != null ? promotion.getUser().getName() : null)
                .createdAt(promotion.getCreatedAt())
                .build();
    }

    private PromotionPackageResponse mapPackageToResponse(PromotionPackage pkg) {
        return PromotionPackageResponse.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .price(pkg.getPrice())
                .durationDays(pkg.getDurationDays())
                .dailyReach(pkg.getDailyReach())
                .totalImpressions(pkg.getTotalImpressions())
                .promotionType(pkg.getPromotionType())
                .includesTargeting(pkg.getIncludesTargeting())
                .includesAnalytics(pkg.getIncludesAnalytics())
                .build();
    }
}
