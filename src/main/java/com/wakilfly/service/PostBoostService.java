package com.wakilfly.service;

import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.*;
import com.wakilfly.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostBoostService {

    private final SystemSettingsService systemSettingsService;
    private final PaymentService paymentService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PromotionRepository promotionRepository;
    private final BusinessRepository businessRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Get ads price per person from system settings
     */
    public BigDecimal getAdsPricePerPerson() {
        return systemSettingsService.getAdsPricePerPerson();
    }

    /**
     * Create a post boost campaign
     * Creates a promotion and initiates USSD payment
     */
    @Transactional
    public Map<String, Object> createPostBoost(UUID userId, UUID postId, long targetReach, String paymentPhone) {
        // Validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Validate post exists and belongs to user
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new BadRequestException("You can only boost your own posts");
        }

        // Validate target reach
        if (targetReach < 1) {
            throw new BadRequestException("Target reach must be at least 1 person");
        }

        if (targetReach > 1000000) {
            throw new BadRequestException("Target reach cannot exceed 1,000,000 people");
        }

        // Calculate price
        BigDecimal pricePerPerson = getAdsPricePerPerson();
        BigDecimal totalPrice = pricePerPerson.multiply(BigDecimal.valueOf(targetReach));

        // Get business if user has one
        Business business = businessRepository.findByOwnerId(userId).orElse(null);

        // Create promotion
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(30); // Default 30 days campaign

        Promotion promotion = Promotion.builder()
                .user(user)
                .business(business)
                .type(PromotionType.POST)
                .targetId(postId)
                .title("Boost Post: " + (post.getCaption() != null && post.getCaption().length() > 50 
                        ? post.getCaption().substring(0, 50) + "..." 
                        : post.getCaption() != null ? post.getCaption() : "Post"))
                .description("Boost post to reach " + targetReach + " people")
                .budget(totalPrice)
                .startDate(now)
                .endDate(endDate)
                .status(PromotionStatus.PENDING)
                .reach(targetReach)
                .build();

        promotion = promotionRepository.save(promotion);

        // Initiate USSD payment
        String description = "Post boost: Reach " + targetReach + " people";
        String orderId = paymentService.initiatePayment(
                userId,
                totalPrice,
                PaymentType.PROMOTION,
                paymentPhone,
                description,
                promotion.getId(),
                "PROMOTION"
        );

        // Update promotion with payment ID
        Payment payment = paymentRepository.findByTransactionId(orderId).orElse(null);
        if (payment != null) {
            promotion.setPaymentId(payment.getId());
            promotionRepository.save(promotion);
        }

        log.info("Post boost created: promotionId={}, postId={}, targetReach={}, price={}, orderId={}",
                promotion.getId(), postId, targetReach, totalPrice, orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("promotionId", promotion.getId());
        response.put("orderId", orderId);
        response.put("targetReach", targetReach);
        response.put("totalPrice", totalPrice);
        response.put("pricePerPerson", pricePerPerson);
        response.put("message", "USSD push imetumwa kwa simu yako. Fuata maelekezo kukamilisha malipo.");

        return response;
    }

    /**
     * Get boost analytics for a user
     */
    public Map<String, Object> getBoostAnalytics(UUID userId) {
        List<Promotion> promotions = promotionRepository.findByUserIdOrderByCreatedAtDesc(
                userId, 
                org.springframework.data.domain.PageRequest.of(0, 100)
        ).getContent();

        // Filter only POST type promotions
        List<Promotion> postPromotions = promotions.stream()
                .filter(p -> p.getType() == PromotionType.POST)
                .collect(java.util.stream.Collectors.toList());

        // Calculate totals
        long totalImpressions = postPromotions.stream()
                .mapToLong(p -> p.getImpressions() != null ? p.getImpressions() : 0L)
                .sum();
        
        long totalClicks = postPromotions.stream()
                .mapToLong(p -> p.getClicks() != null ? p.getClicks() : 0L)
                .sum();
        
        long totalReach = postPromotions.stream()
                .mapToLong(p -> p.getReach() != null ? p.getReach() : 0L)
                .sum();
        
        BigDecimal totalSpent = postPromotions.stream()
                .map(p -> p.getSpentAmount() != null ? p.getSpentAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalBudget = postPromotions.stream()
                .map(Promotion::getBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate CTR
        double ctr = totalImpressions > 0 ? (totalClicks * 100.0 / totalImpressions) : 0.0;

        // Active promotions
        long activeCount = postPromotions.stream()
                .filter(p -> p.getStatus() == PromotionStatus.ACTIVE)
                .count();

        // Build response with individual promotion stats
        List<Map<String, Object>> promotionStats = postPromotions.stream()
                .map(p -> {
                    Map<String, Object> stat = new HashMap<>();
                    stat.put("id", p.getId());
                    stat.put("postId", p.getTargetId());
                    stat.put("title", p.getTitle());
                    stat.put("status", p.getStatus().toString());
                    stat.put("targetReach", p.getReach());
                    stat.put("impressions", p.getImpressions() != null ? p.getImpressions() : 0L);
                    stat.put("clicks", p.getClicks() != null ? p.getClicks() : 0L);
                    stat.put("conversions", p.getConversions() != null ? p.getConversions() : 0L);
                    stat.put("budget", p.getBudget());
                    stat.put("spentAmount", p.getSpentAmount() != null ? p.getSpentAmount() : BigDecimal.ZERO);
                    stat.put("ctr", p.getCtr() != null ? p.getCtr() : 0.0);
                    stat.put("startDate", p.getStartDate());
                    stat.put("endDate", p.getEndDate());
                    stat.put("createdAt", p.getCreatedAt());
                    return stat;
                })
                .collect(java.util.stream.Collectors.toList());

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalPromotions", postPromotions.size());
        analytics.put("activePromotions", activeCount);
        analytics.put("totalImpressions", totalImpressions);
        analytics.put("totalClicks", totalClicks);
        analytics.put("totalReach", totalReach);
        analytics.put("totalSpent", totalSpent);
        analytics.put("totalBudget", totalBudget);
        analytics.put("overallCtr", ctr);
        analytics.put("promotions", promotionStats);

        return analytics;
    }
}
