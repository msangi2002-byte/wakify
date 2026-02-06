package com.wakilfly.service;

import com.wakilfly.dto.request.CreateSubscriptionRequest;
import com.wakilfly.dto.response.SubscriptionPlansResponse;
import com.wakilfly.dto.response.SubscriptionResponse;
import com.wakilfly.model.*;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.repository.BusinessRepository;
import com.wakilfly.repository.PaymentRepository;
import com.wakilfly.repository.SubscriptionRepository;
import com.wakilfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final BusinessRepository businessRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    // Subscription pricing (in TSh)
    public static final BigDecimal WEEKLY_PRICE = new BigDecimal("5000"); // 5,000/=
    public static final BigDecimal MONTHLY_PRICE = new BigDecimal("15000"); // 15,000/= (25% savings)
    public static final BigDecimal QUARTERLY_PRICE = new BigDecimal("40000"); // 40,000/= (33% savings)
    public static final BigDecimal ANNUAL_PRICE = new BigDecimal("150000"); // 150,000/= (42% savings)

    /**
     * Get available subscription plans
     */
    public SubscriptionPlansResponse getPlans() {
        return SubscriptionPlansResponse.builder()
                .plans(Arrays.asList(
                        buildPlanInfo(SubscriptionPlan.WEEKLY, "Wiki 1",
                                "Jaribu huduma zetu kwa wiki moja", WEEKLY_PRICE, 7, BigDecimal.ZERO),
                        buildPlanInfo(SubscriptionPlan.MONTHLY, "Mwezi 1",
                                "Mpango maarufu zaidi - okoa 25%", MONTHLY_PRICE, 30,
                                new BigDecimal("25")),
                        buildPlanInfo(SubscriptionPlan.QUARTERLY, "Miezi 3",
                                "Bora kwa biashara inayokua - okoa 33%", QUARTERLY_PRICE, 90,
                                new BigDecimal("33")),
                        buildPlanInfo(SubscriptionPlan.ANNUAL, "Mwaka 1",
                                "Thamani bora - okoa 42%", ANNUAL_PRICE, 365,
                                new BigDecimal("42"))))
                .build();
    }

    /**
     * Create or renew subscription
     */
    @Transactional
    public SubscriptionResponse createSubscription(UUID businessOwnerId, CreateSubscriptionRequest request) {
        // Get business
        Business business = businessRepository.findByOwnerId(businessOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found for this user"));

        // Check if business already has active subscription
        Optional<Subscription> existingSubscription = subscriptionRepository.findByBusinessId(business.getId());
        if (existingSubscription.isPresent() && existingSubscription.get().isActive()) {
            throw new BadRequestException("Business already has an active subscription");
        }

        // Get price for plan
        BigDecimal amount = getPriceForPlan(request.getPlan());
        int durationDays = getDurationDays(request.getPlan());

        // Create subscription (pending until payment)
        Subscription subscription = existingSubscription.orElse(
                Subscription.builder()
                        .business(business)
                        .build());

        subscription.setPlan(request.getPlan());
        subscription.setAmount(amount);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setAutoRenew(request.getAutoRenew() != null && request.getAutoRenew());

        subscription = subscriptionRepository.save(subscription);

        // Create payment record
        Payment payment = Payment.builder()
                .user(business.getOwner())
                .transactionId("PAY" + System.currentTimeMillis())
                .type(PaymentType.SUBSCRIPTION)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .paymentPhone(request.getPaymentPhone())
                .description("Subscription: " + request.getPlan().name())
                .relatedEntityType("SUBSCRIPTION")
                .relatedEntityId(subscription.getId())
                .build();

        paymentRepository.save(payment);

        log.info("Subscription {} created for business {}, awaiting payment",
                subscription.getId(), business.getId());

        // TODO: Initiate mobile money payment

        return mapToSubscriptionResponse(subscription);
    }

    /**
     * Activate subscription after payment
     */
    @Transactional
    public SubscriptionResponse activateSubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            throw new BadRequestException("Subscription is already active");
        }

        int durationDays = getDurationDays(subscription.getPlan());
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(durationDays);

        // If extending existing subscription
        if (subscription.getEndDate() != null && subscription.getEndDate().isAfter(startDate)) {
            endDate = subscription.getEndDate().plusDays(durationDays);
        }

        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setReminderSent7Days(false);
        subscription.setReminderSent3Days(false);
        subscription.setReminderSent1Day(false);

        // Activate the business
        Business business = subscription.getBusiness();
        business.setStatus(BusinessStatus.ACTIVE);
        businessRepository.save(business);

        subscription = subscriptionRepository.save(subscription);

        log.info("Subscription {} activated until {}", subscriptionId, endDate);

        return mapToSubscriptionResponse(subscription);
    }

    /**
     * Get subscription for business
     */
    public SubscriptionResponse getSubscription(UUID businessOwnerId) {
        Business business = businessRepository.findByOwnerId(businessOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found for this user"));

        Subscription subscription = subscriptionRepository.findByBusinessId(business.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No subscription found"));

        return mapToSubscriptionResponse(subscription);
    }

    /**
     * Cancel subscription
     */
    @Transactional
    public SubscriptionResponse cancelSubscription(UUID businessOwnerId) {
        Business business = businessRepository.findByOwnerId(businessOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found for this user"));

        Subscription subscription = subscriptionRepository.findByBusinessId(business.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No subscription found"));

        subscription.setAutoRenew(false);
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription = subscriptionRepository.save(subscription);

        log.info("Subscription {} cancelled", subscription.getId());

        return mapToSubscriptionResponse(subscription);
    }

    /**
     * Toggle auto-renew
     */
    @Transactional
    public SubscriptionResponse toggleAutoRenew(UUID businessOwnerId, boolean autoRenew) {
        Business business = businessRepository.findByOwnerId(businessOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found for this user"));

        Subscription subscription = subscriptionRepository.findByBusinessId(business.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No subscription found"));

        subscription.setAutoRenew(autoRenew);
        subscription = subscriptionRepository.save(subscription);

        log.info("Subscription {} auto-renew set to {}", subscription.getId(), autoRenew);

        return mapToSubscriptionResponse(subscription);
    }

    /**
     * Scheduled task: Check for expired subscriptions
     */
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional
    public void checkExpiredSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> expiredSubscriptions = subscriptionRepository.findExpiredSubscriptions(now);

        for (Subscription subscription : expiredSubscriptions) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);

            // Update business status
            Business business = subscription.getBusiness();
            business.setStatus(BusinessStatus.INACTIVE);
            businessRepository.save(business);

            subscriptionRepository.save(subscription);

            log.info("Subscription {} expired for business {}",
                    subscription.getId(), business.getId());

            // TODO: Send expiry notification
        }
    }

    /**
     * Scheduled task: Send renewal reminders
     */
    @Scheduled(cron = "0 0 9 * * *") // Run at 9 AM every day
    @Transactional
    public void sendRenewalReminders() {
        LocalDateTime now = LocalDateTime.now();

        // 7-day reminder
        List<Subscription> subs7Days = subscriptionRepository.findSubscriptionsExpiringIn7Days(
                now, now.plusDays(7));
        for (Subscription sub : subs7Days) {
            // TODO: Send 7-day reminder notification
            sub.setReminderSent7Days(true);
            subscriptionRepository.save(sub);
            log.info("Sent 7-day reminder for subscription {}", sub.getId());
        }

        // 3-day reminder
        List<Subscription> subs3Days = subscriptionRepository.findSubscriptionsExpiringIn3Days(
                now, now.plusDays(3));
        for (Subscription sub : subs3Days) {
            // TODO: Send 3-day reminder notification
            sub.setReminderSent3Days(true);
            subscriptionRepository.save(sub);
            log.info("Sent 3-day reminder for subscription {}", sub.getId());
        }

        // 1-day reminder
        List<Subscription> subs1Day = subscriptionRepository.findSubscriptionsExpiringIn1Day(
                now, now.plusDays(1));
        for (Subscription sub : subs1Day) {
            // TODO: Send 1-day reminder notification
            sub.setReminderSent1Day(true);
            subscriptionRepository.save(sub);
            log.info("Sent 1-day reminder for subscription {}", sub.getId());
        }
    }

    // Helper methods

    private BigDecimal getPriceForPlan(SubscriptionPlan plan) {
        return switch (plan) {
            case WEEKLY -> WEEKLY_PRICE;
            case MONTHLY -> MONTHLY_PRICE;
            case QUARTERLY -> QUARTERLY_PRICE;
            case ANNUAL -> ANNUAL_PRICE;
        };
    }

    private int getDurationDays(SubscriptionPlan plan) {
        return switch (plan) {
            case WEEKLY -> 7;
            case MONTHLY -> 30;
            case QUARTERLY -> 90;
            case ANNUAL -> 365;
        };
    }

    private SubscriptionPlansResponse.PlanInfo buildPlanInfo(
            SubscriptionPlan plan, String name, String description,
            BigDecimal price, int durationDays, BigDecimal savings) {

        return SubscriptionPlansResponse.PlanInfo.builder()
                .plan(plan)
                .name(name)
                .description(description)
                .price(price)
                .durationDays(durationDays)
                .savingsPercentage(savings)
                .features(getFeatures(plan))
                .build();
    }

    private List<String> getFeatures(SubscriptionPlan plan) {
        List<String> baseFeatures = Arrays.asList(
                "Weka bidhaa zako bila kikomo",
                "Pata mawasiliano na wateja",
                "Tazamwa na maelfu ya wateja",
                "Pata taarifa za mauzo");

        return switch (plan) {
            case WEEKLY -> baseFeatures;
            case MONTHLY -> {
                List<String> features = new java.util.ArrayList<>(baseFeatures);
                features.add("Msaada wa kipaumbele");
                yield features;
            }
            case QUARTERLY -> {
                List<String> features = new java.util.ArrayList<>(baseFeatures);
                features.add("Msaada wa kipaumbele");
                features.add("Onyeshwa kwenye trending");
                yield features;
            }
            case ANNUAL -> {
                List<String> features = new java.util.ArrayList<>(baseFeatures);
                features.add("Msaada wa kipaumbele 24/7");
                features.add("Onyeshwa kwenye trending");
                features.add("Alama ya kuaminika (Verified)");
                features.add("Uchambuzi wa kina wa biashara");
                yield features;
            }
        };
    }

    private SubscriptionResponse mapToSubscriptionResponse(Subscription subscription) {
        Business business = subscription.getBusiness();
        long daysRemaining = subscription.getEndDate() != null ? Math.max(0, subscription.daysUntilExpiry()) : 0;

        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .plan(subscription.getPlan())
                .status(subscription.getStatus())
                .amount(subscription.getAmount())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .daysRemaining(daysRemaining)
                .autoRenew(subscription.getAutoRenew())
                .businessId(business.getId())
                .businessName(business.getName())
                .isActive(subscription.isActive())
                .isExpired(subscription.isExpired())
                .isInGracePeriod(subscription.getStatus() == SubscriptionStatus.GRACE)
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}
