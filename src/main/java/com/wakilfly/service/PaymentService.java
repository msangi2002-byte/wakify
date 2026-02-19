package com.wakilfly.service;

import com.wakilfly.model.*;
import com.wakilfly.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AgentRepository agentRepository;
    private final AgentPackageRepository agentPackageRepository;
    private final BusinessRepository businessRepository;
    private final BusinessRequestRepository businessRequestRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PromotionRepository promotionRepository;
    private final CommissionRepository commissionRepository;
    private final GiftService giftService;
    private final AuditLogService auditLogService;
    private final SystemSettingsService systemSettingsService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${payment.harakapay.api-key:hpk_2f21cdc1056625d67e02e5de750b0616ddc8d51ce0c23810}")
    private String harakaPayApiKey;

    @Value("${payment.harakapay.base-url:https://harakapay.net}")
    private String harakaPayBaseUrl;

    /**
     * Initiate payment via HarakaPay
     * This works for M-Pesa, Tigo Pesa, Airtel Money, Halo Pesa
     */
    @Transactional
    public String initiatePayment(UUID userId, BigDecimal amount, PaymentType type, String phone, String description) {
        return initiatePayment(userId, amount, type, phone, description, null);
    }

    @Transactional
    public String initiatePayment(UUID userId, BigDecimal amount, PaymentType type, String phone, String description,
            UUID relatedEntityId) {
        return initiatePayment(userId, amount, type, phone, description, relatedEntityId, null);
    }

    @Transactional
    public String initiatePayment(UUID userId, BigDecimal amount, PaymentType type, String phone, String description,
            UUID relatedEntityId, String relatedEntityType) {
        User user = userRepository.findById(userId).orElseThrow();

        // Create payment record
        Payment payment = Payment.builder()
                .user(user)
                .amount(amount)
                .type(type)
                .status(PaymentStatus.PENDING)
                .method(detectPaymentMethod(phone))
                .paymentPhone(phone)
                .description(description)
                .relatedEntityId(relatedEntityId)
                .relatedEntityType(relatedEntityType)
                .build();

        payment = paymentRepository.save(payment);

        // Demo mode: no USSD push; mark payment SUCCESS and process immediately
        if (systemSettingsService.getPaymentDemoMode()) {
            String orderId = "DEMO-" + payment.getId().toString();
            payment.setTransactionId(orderId);
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            payment.setProviderResponse("demo_mode=true");
            paymentRepository.save(payment);
            processSuccessfulPayment(payment);
            log.info("Payment demo mode: payment {} marked SUCCESS (no USSD), orderId {}", payment.getId(), orderId);
            return orderId;
        }

        // Call HarakaPay API
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", harakaPayApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("phone", formatPhone(phone));
            requestBody.put("amount", amount.intValue());
            requestBody.put("description", description);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    harakaPayBaseUrl + "/api/v1/collect",
                    HttpMethod.POST,
                    entity,
                    Map.class);

            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && Boolean.TRUE.equals(responseBody.get("success"))) {
                String orderId = (String) responseBody.get("order_id");
                payment.setTransactionId(orderId);
                payment.setExternalReference(orderId);

                // Store fee info
                if (responseBody.get("fee") != null) {
                    payment.setProviderResponse("fee=" + responseBody.get("fee") +
                            ", net_amount=" + responseBody.get("net_amount"));
                }

                paymentRepository.save(payment);
                log.info("HarakaPay payment initiated: {} for {} TZS, Order ID: {}",
                        payment.getId(), amount, orderId);

                return orderId;
            } else {
                String error = responseBody != null ? (String) responseBody.get("error") : "Unknown error";
                payment.setStatus(PaymentStatus.FAILED);
                payment.setProviderResponse(error);
                paymentRepository.save(payment);
                log.error("HarakaPay payment failed: {}", error);
                throw new RuntimeException("Payment failed: " + error);
            }

        } catch (Exception e) {
            log.error("Failed to initiate HarakaPay payment", e);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setProviderResponse(e.getMessage());
            paymentRepository.save(payment);
            throw new RuntimeException("Failed to initiate payment: " + e.getMessage());
        }
    }

    /**
     * Check payment status from HarakaPay
     */
    public Map<String, Object> checkPaymentStatus(String orderId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", harakaPayApiKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    harakaPayBaseUrl + "/api/v1/status/" + orderId,
                    HttpMethod.GET,
                    entity,
                    Map.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to check payment status for order {}", orderId, e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Get HarakaPay wallet balance
     */
    public Map<String, Object> getWalletBalance() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", harakaPayApiKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    harakaPayBaseUrl + "/api/v1/balance",
                    HttpMethod.GET,
                    entity,
                    Map.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get wallet balance", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Scheduled job to check pending payments every 30 seconds
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void checkPendingPayments() {
        List<Payment> pendingPayments = paymentRepository.findByStatus(PaymentStatus.PENDING);

        for (Payment payment : pendingPayments) {
            if (payment.getTransactionId() == null) {
                continue;
            }

            // Skip payments created less than 10 seconds ago
            if (payment.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(10))) {
                continue;
            }

            try {
                Map<String, Object> statusResponse = checkPaymentStatus(payment.getTransactionId());

                if (statusResponse != null && Boolean.TRUE.equals(statusResponse.get("success"))) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> paymentData = (Map<String, Object>) statusResponse.get("payment");

                    if (paymentData != null) {
                        String status = (String) paymentData.get("status");

                        if ("completed".equalsIgnoreCase(status)) {
                            payment.setStatus(PaymentStatus.SUCCESS);
                            payment.setPaidAt(LocalDateTime.now());
                            payment.setProviderResponse(statusResponse.toString());
                            paymentRepository.save(payment);

                            // Process successful payment
                            processSuccessfulPayment(payment);

                            log.info("Payment {} completed successfully", payment.getTransactionId());

                        } else if ("failed".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status)) {
                            payment.setStatus(PaymentStatus.FAILED);
                            payment.setProviderResponse(statusResponse.toString());
                            paymentRepository.save(payment);

                            log.info("Payment {} failed: {}", payment.getTransactionId(), status);
                        }
                        // If still pending, we'll check again next cycle
                    }
                }
            } catch (Exception e) {
                log.error("Error checking payment status for {}", payment.getTransactionId(), e);
            }
        }
    }

    /**
     * Manual payment status refresh (for user-initiated checks)
     */
    @Transactional
    public Payment refreshPaymentStatus(String orderId) {
        Payment payment = paymentRepository.findByTransactionId(orderId).orElse(null);
        if (payment == null) {
            return null;
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return payment; // Already processed
        }

        Map<String, Object> statusResponse = checkPaymentStatus(orderId);

        if (statusResponse != null && Boolean.TRUE.equals(statusResponse.get("success"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> paymentData = (Map<String, Object>) statusResponse.get("payment");

            if (paymentData != null) {
                String status = (String) paymentData.get("status");

                if ("completed".equalsIgnoreCase(status)) {
                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setPaidAt(LocalDateTime.now());
                    payment.setProviderResponse(statusResponse.toString());
                    paymentRepository.save(payment);
                    processSuccessfulPayment(payment);
                } else if ("failed".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status)) {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setProviderResponse(statusResponse.toString());
                    paymentRepository.save(payment);
                }
            }
        }

        return payment;
    }

    /**
     * Process successful payment based on type
     */
    private void processSuccessfulPayment(Payment payment) {
        switch (payment.getType()) {
            case AGENT_REGISTRATION -> activateAgent(payment);
            case BUSINESS_ACTIVATION -> activateBusiness(payment);
            case SUBSCRIPTION -> activateSubscription(payment);
            case PROMOTION -> activatePromotion(payment);
            case ORDER -> processOrderPayment(payment);
            case COIN_PURCHASE -> processCoinPurchase(payment);
            case AGENT_PACKAGE -> activateAgentPackage(payment);
            default -> log.info("Payment type {} does not require additional processing", payment.getType());
        }
    }

    private void processCoinPurchase(Payment payment) {
        if (payment.getRelatedEntityId() != null) {
            try {
                giftService.purchaseCoins(payment.getUser().getId(), payment.getRelatedEntityId());
                log.info("Coin purchase processed for user {} with package {}",
                        payment.getUser().getId(), payment.getRelatedEntityId());
            } catch (Exception e) {
                log.error("Failed to process coin purchase for user {}: {}",
                        payment.getUser().getId(), e.getMessage());
            }
        }
    }

    private void activateAgent(Payment payment) {
        Agent agent = agentRepository.findByUserId(payment.getUser().getId()).orElse(null);
        if (agent != null && agent.getStatus() == AgentStatus.PENDING) {
            agent.setStatus(AgentStatus.ACTIVE);
            agent.setApprovedAt(LocalDateTime.now());
            agentRepository.save(agent);
            log.info("Agent {} activated after payment {}", agent.getAgentCode(), payment.getTransactionId());
        }
    }

    private void activateAgentPackage(Payment payment) {
        if (payment.getRelatedEntityId() == null) {
            log.warn("Agent package payment {} has no related entity ID", payment.getId());
            return;
        }

        Agent agent = agentRepository.findByUserId(payment.getUser().getId()).orElse(null);
        if (agent == null) {
            log.warn("Agent not found for user {} in payment {}", payment.getUser().getId(), payment.getId());
            return;
        }

        try {
            AgentPackage agentPackage = agentPackageRepository.findById(payment.getRelatedEntityId()).orElse(null);
            if (agentPackage == null) {
                log.warn("Agent package {} not found for payment {}", payment.getRelatedEntityId(), payment.getId());
                return;
            }

            if (!agentPackage.getIsActive()) {
                log.warn("Agent package {} is not active, cannot assign to agent {}", agentPackage.getId(), agent.getAgentCode());
                return;
            }

            // If agent was just registered with this package (PENDING), activate them now
            if (agent.getStatus() == AgentStatus.PENDING) {
                agent.setStatus(AgentStatus.ACTIVE);
                agent.setApprovedAt(LocalDateTime.now());
                log.info("Agent {} activated after package payment {}", agent.getAgentCode(), payment.getTransactionId());
            }

            // Assign package to agent
            agent.setAgentPackage(agentPackage);
            agentRepository.save(agent);

            log.info("Agent {} purchased/upgraded to package {} after payment {}",
                    agent.getAgentCode(), agentPackage.getName(), payment.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to activate agent package for payment {}: {}", payment.getId(), e.getMessage(), e);
        }
    }

    private void activateBusiness(Payment payment) {
        // System flow: user with account requested business → paid via USSD → create business and approve user
        if ("BUSINESS_REQUEST".equals(payment.getRelatedEntityType()) && payment.getRelatedEntityId() != null) {
            activateBusinessFromRequest(payment);
            return;
        }
        // Agent flow: agent registered new user (no account) and created business; payment confirms → activate business
        Business business = businessRepository.findByOwnerId(payment.getUser().getId()).orElse(null);
        if (business != null && business.getStatus() == BusinessStatus.PENDING) {
            business.setStatus(BusinessStatus.ACTIVE);
            businessRepository.save(business);

            // Create commission for agent who activated the business
            if (business.getAgent() != null) {
                Agent agent = business.getAgent();
                BigDecimal commissionAmount = new BigDecimal("5000.00");

                // Check if commission already exists for this business and agent
                boolean commissionExists = commissionRepository.findByAgentIdOrderByCreatedAtDesc(agent.getId(), 
                        PageRequest.of(0, 100))
                        .getContent()
                        .stream()
                        .anyMatch(c -> c.getBusiness() != null && c.getBusiness().getId().equals(business.getId()) 
                                && (c.getType() == CommissionType.BUSINESS_ACTIVATION || c.getType() == CommissionType.ACTIVATION));

                if (!commissionExists) {
                    Commission commission = Commission.builder()
                            .agent(agent)
                            .business(business)
                            .amount(commissionAmount)
                            .type(CommissionType.BUSINESS_ACTIVATION)
                            .description("Commission for activating business: " + business.getName())
                            .status(CommissionStatus.PAID)
                            .paidAt(LocalDateTime.now())
                            .build();
                    commissionRepository.save(commission);

                    agent.addEarnings(commissionAmount);
                    agent.setBusinessesActivated(agent.getBusinessesActivated() + 1);
                    agentRepository.save(agent);

                    log.info("Agent {} earned {} TZS commission for business {}",
                            agent.getAgentCode(), commissionAmount, business.getName());
                } else {
                    log.info("Commission already exists for business {}, skipping commission creation", business.getName());
                }
            }

            User owner = payment.getUser();
            if (owner.getReferredByAgentCode() != null && !owner.getReferredByAgentCode().isEmpty()) {
                Agent referringAgent = agentRepository.findByAgentCode(owner.getReferredByAgentCode()).orElse(null);
                if (referringAgent != null && !referringAgent.equals(business.getAgent())) {
                    BigDecimal referralBonus = new BigDecimal("2000.00");
                    Commission referralCommission = Commission.builder()
                            .agent(referringAgent)
                            .business(business)
                            .amount(referralBonus)
                            .type(CommissionType.REFERRAL)
                            .description("Referral bonus for user " + owner.getPhone() + " starting business: "
                                    + business.getName())
                            .status(CommissionStatus.PAID)
                            .paidAt(LocalDateTime.now())
                            .build();
                    commissionRepository.save(referralCommission);
                    referringAgent.addEarnings(referralBonus);
                    referringAgent.setTotalReferrals(referringAgent.getTotalReferrals() + 1);
                    agentRepository.save(referringAgent);
                    log.info("Agent {} earned {} TZS referral bonus for user {} who started business {}",
                            referringAgent.getAgentCode(), referralBonus, owner.getPhone(), business.getName());
                }
            }

            log.info("Business {} activated after payment {}", business.getName(), payment.getTransactionId());
        }
    }

    /** After user (with account) pays business activation via USSD. If request has agent: set PAID (agent must approve). Else: create business now. */
    private void activateBusinessFromRequest(Payment payment) {
        BusinessRequest br = businessRequestRepository.findById(payment.getRelatedEntityId()).orElse(null);
        if (br == null || br.getStatus() != BusinessRequestStatus.PENDING) {
            log.warn("BusinessRequest {} not found or not PENDING, skipping activateBusinessFromRequest", payment.getRelatedEntityId());
            return;
        }
        User owner = br.getUser();
        if (businessRepository.findByOwnerId(owner.getId()).isPresent()) {
            br.setStatus(BusinessRequestStatus.CONVERTED);
            businessRequestRepository.save(br);
            return; // already has business
        }
        // When user selected an agent: wait for agent to visit and approve; do not create business yet
        if (br.getAgent() != null) {
            br.setStatus(BusinessRequestStatus.PAID);
            businessRequestRepository.save(br);
            log.info("Business request {} marked PAID; agent {} must visit and approve", br.getId(), br.getAgent().getAgentCode());
            return;
        }
        doCreateBusinessFromRequest(br);
    }

    /**
     * Create business from request and set CONVERTED. Used when: (1) payment completed and no agent, or (2) agent approved after visit.
     */
    private void doCreateBusinessFromRequest(BusinessRequest br) {
        User owner = br.getUser();
        if (businessRepository.findByOwnerId(owner.getId()).isPresent()) {
            br.setStatus(BusinessRequestStatus.CONVERTED);
            businessRequestRepository.save(br);
            return;
        }
        Business business = Business.builder()
                .name(br.getBusinessName())
                .description(br.getDescription())
                .category(br.getCategory())
                .owner(owner)
                .agent(br.getAgent())
                .region(br.getRegion())
                .district(br.getDistrict())
                .ward(br.getWard())
                .street(br.getStreet())
                .status(BusinessStatus.ACTIVE)
                .isVerified(false)
                .build();
        business = businessRepository.save(business);
        owner.setRole(Role.BUSINESS);
        userRepository.save(owner);
        br.setStatus(BusinessRequestStatus.CONVERTED);
        businessRequestRepository.save(br);

        // Create commission for agent who activated the business
        if (business.getAgent() != null) {
            Agent agent = business.getAgent();
            BigDecimal commissionAmount = new BigDecimal("5000.00");
            final Business savedBusiness = business;

            // Check if commission already exists for this business and agent
            boolean commissionExists = commissionRepository.findByAgentIdOrderByCreatedAtDesc(agent.getId(), 
                    PageRequest.of(0, 100))
                    .getContent()
                    .stream()
                    .anyMatch(c -> c.getBusiness() != null && c.getBusiness().getId().equals(savedBusiness.getId()) 
                            && (c.getType() == CommissionType.BUSINESS_ACTIVATION || c.getType() == CommissionType.ACTIVATION));

            if (!commissionExists) {
                Commission commission = Commission.builder()
                        .agent(agent)
                        .business(business)
                        .amount(commissionAmount)
                        .type(CommissionType.BUSINESS_ACTIVATION)
                        .description("Commission for activating business from request: " + business.getName())
                        .status(CommissionStatus.PAID)
                        .paidAt(LocalDateTime.now())
                        .build();
                commissionRepository.save(commission);

                agent.addEarnings(commissionAmount);
                agent.setBusinessesActivated(agent.getBusinessesActivated() + 1);
                agentRepository.save(agent);

                log.info("Agent {} earned {} TZS commission for business {} (from request)",
                        agent.getAgentCode(), commissionAmount, business.getName());
            } else {
                log.info("Commission already exists for business {}, skipping commission creation", business.getName());
            }

            // Handle referral bonus if applicable
            if (owner.getReferredByAgentCode() != null && !owner.getReferredByAgentCode().isEmpty()) {
                Agent referringAgent = agentRepository.findByAgentCode(owner.getReferredByAgentCode()).orElse(null);
                if (referringAgent != null && !referringAgent.equals(business.getAgent())) {
                    BigDecimal referralBonus = new BigDecimal("2000.00");
                    Commission referralCommission = Commission.builder()
                            .agent(referringAgent)
                            .business(business)
                            .amount(referralBonus)
                            .type(CommissionType.REFERRAL)
                            .description("Referral bonus for user " + owner.getPhone() + " starting business: "
                                    + business.getName())
                            .status(CommissionStatus.PAID)
                            .paidAt(LocalDateTime.now())
                            .build();
                    commissionRepository.save(referralCommission);
                    referringAgent.addEarnings(referralBonus);
                    referringAgent.setTotalReferrals(referringAgent.getTotalReferrals() + 1);
                    agentRepository.save(referringAgent);
                    log.info("Agent {} earned {} TZS referral bonus for user {} who started business {}",
                            referringAgent.getAgentCode(), referralBonus, owner.getPhone(), business.getName());
                }
            }
        }

        log.info("Business {} created and user {} approved (request {})",
                business.getName(), owner.getId(), br.getId());
    }

    /**
     * Complete business registration from request. Called when agent approves after visit (request status must be PAID).
     */
    @Transactional
    public void completeBusinessFromRequest(BusinessRequest br) {
        doCreateBusinessFromRequest(br);
    }

    private void activateSubscription(Payment payment) {
        Business business = businessRepository.findByOwnerId(payment.getUser().getId()).orElse(null);
        if (business != null) {
            Subscription subscription = subscriptionRepository
                    .findByBusinessIdAndStatus(business.getId(), SubscriptionStatus.PENDING)
                    .orElse(null);

            if (subscription != null) {
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscription.setStartDate(LocalDateTime.now());
                int days = getSubscriptionDuration(subscription.getPlan());
                subscription.setEndDate(LocalDateTime.now().plusDays(days));
                subscriptionRepository.save(subscription);
                log.info("Subscription {} activated for business {}, valid for {} days",
                        subscription.getId(), business.getName(), days);
            }
        }
    }

    private int getSubscriptionDuration(SubscriptionPlan plan) {
        return switch (plan) {
            case WEEKLY -> 7;
            case MONTHLY -> 30;
            case QUARTERLY -> 90;
            case ANNUAL -> 365;
        };
    }

    private void activatePromotion(Payment payment) {
        Promotion promotion = promotionRepository.findAll().stream()
                .filter(p -> p.getPaymentId() != null && p.getPaymentId().equals(payment.getId()))
                .findFirst()
                .orElse(null);

        if (promotion != null) {
            promotion.setIsPaid(true);
            // Require admin approval before going live (policy check)
            promotion.setStatus(PromotionStatus.PENDING_APPROVAL);
            promotionRepository.save(promotion);
            log.info("Promotion {} activated after payment {}", promotion.getId(), payment.getTransactionId());
        }
    }

    private void processOrderPayment(Payment payment) {
        // Order payment processing - update order status
        log.info("Order payment {} processed successfully", payment.getTransactionId());
    }

    /**
     * Get payment history for user (profile – historia ya malipo: coins, subscription, n.k.)
     */
    public org.springframework.data.domain.Page<Payment> getMyPayments(UUID userId,
            org.springframework.data.domain.Pageable pageable) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get payment by order ID
     */
    public Payment getPaymentByOrderId(String orderId) {
        return paymentRepository.findByTransactionId(orderId).orElse(null);
    }

    /**
     * Get payment by ID
     */
    public Payment getPaymentById(UUID paymentId) {
        return paymentRepository.findById(paymentId).orElse(null);
    }

    /**
     * Detect payment method from phone number
     */
    private PaymentMethod detectPaymentMethod(String phone) {
        String formatted = formatPhone(phone);

        // M-Pesa: 065, 067, 068 (Vodafone)
        if (formatted.startsWith("2556") || formatted.startsWith("25567") || formatted.startsWith("25568")) {
            return PaymentMethod.MPESA;
        }
        // Tigo Pesa: 071, 065 (Tigo)
        if (formatted.startsWith("25571") || formatted.startsWith("25565")) {
            return PaymentMethod.TIGOPESA;
        }
        // Airtel Money: 068, 069, 078 (Airtel)
        if (formatted.startsWith("25568") || formatted.startsWith("25569") || formatted.startsWith("25578")) {
            return PaymentMethod.AIRTELMONEY;
        }
        // Halo Pesa: 062 (Halotel)
        if (formatted.startsWith("25562")) {
            return PaymentMethod.HALOPESA;
        }

        return PaymentMethod.MPESA; // Default
    }

    /**
     * Format phone to international format
     */
    private String formatPhone(String phone) {
        if (phone == null)
            return "";
        phone = phone.replaceAll("[^0-9]", ""); // Remove non-digits

        if (phone.startsWith("0")) {
            return "255" + phone.substring(1);
        } else if (phone.startsWith("255")) {
            return phone;
        } else if (phone.startsWith("+255")) {
            return phone.substring(1);
        }
        return phone;
    }
}
