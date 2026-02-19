package com.wakilfly.service;

import com.wakilfly.dto.request.AgentRegistrationRequest;
import com.wakilfly.dto.response.AgentRegistrationResponse;
import com.wakilfly.dto.request.BusinessActivationRequest;
import com.wakilfly.dto.request.WithdrawalRequest;
import com.wakilfly.dto.response.*;
import com.wakilfly.model.*;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.repository.*;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

        private final AgentRepository agentRepository;
        private final UserRepository userRepository;
        private final BusinessRepository businessRepository;
        private final CommissionRepository commissionRepository;
        private final PaymentRepository paymentRepository;
        private final WithdrawalRepository withdrawalRepository;
        private final PasswordEncoder passwordEncoder;
        private final SystemSettingsService systemSettingsService;
        private final AgentPackageRepository agentPackageRepository;
        private final PaymentService paymentService;
        private final AgentRatingRepository agentRatingRepository;

        private static final BigDecimal AGENT_COMMISSION = new BigDecimal("5000.00");
        private static final int ONLINE_MINUTES = 15;

        /**
         * Register a user as an Agent.
         * If request.packageId is set: create agent (PENDING), initiate USSD payment for that package; return orderId for frontend to poll until ACTIVE.
         * Otherwise: legacy fixed fee (payment record created, no USSD push yet).
         */
        @Transactional
        public AgentRegistrationResponse registerAsAgent(UUID userId, AgentRegistrationRequest request) {
                // Check if user exists
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

                // Check if already an agent
                if (agentRepository.existsByUserId(userId)) {
                        throw new BadRequestException("You are already registered as an agent");
                }

                // Check if national ID is already used
                if (agentRepository.existsByNationalId(request.getNationalId())) {
                        throw new BadRequestException("National ID is already registered");
                }

                // Generate unique agent code
                String agentCode = generateAgentCode();

                // Create agent record
                Agent agent = Agent.builder()
                                .user(user)
                                .agentCode(agentCode)
                                .nationalId(request.getNationalId())
                                .region(request.getRegion())
                                .district(request.getDistrict())
                                .ward(request.getWard())
                                .latitude(request.getLatitude())
                                .longitude(request.getLongitude())
                                .status(AgentStatus.PENDING) // Pending until payment is confirmed
                                .isVerified(false)
                                .totalEarnings(BigDecimal.ZERO)
                                .availableBalance(BigDecimal.ZERO)
                                .build();

                agent = agentRepository.save(agent);

                // Update user role to AGENT (so they can call GET /agent/me to poll status)
                user.setRole(Role.AGENT);
                userRepository.save(user);

                if (request.getPackageId() != null) {
                        // Package flow: initiate USSD payment; on success PaymentService will activate agent and assign package
                        AgentPackage agentPackage = agentPackageRepository.findById(request.getPackageId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Agent Package", "id", request.getPackageId()));
                        if (!Boolean.TRUE.equals(agentPackage.getIsActive())) {
                                throw new BadRequestException("This package is not available for registration");
                        }
                        String description = "Agent package: " + agentPackage.getName();
                        String orderId = paymentService.initiatePayment(
                                        userId,
                                        agentPackage.getPrice(),
                                        PaymentType.AGENT_PACKAGE,
                                        request.getPaymentPhone(),
                                        description,
                                        agentPackage.getId(),
                                        "AGENT_PACKAGE");
                        log.info("Agent registration with package initiated for user {} with code {}, orderId {}", userId, agentCode, orderId);
                        return AgentRegistrationResponse.builder()
                                        .agent(mapToAgentResponse(agent))
                                        .orderId(orderId)
                                        .build();
                }

                // Legacy flow: fixed registration fee (payment record only; no USSD push in this code path)
                BigDecimal registrationFee = systemSettingsService.getAgentRegisterAmount();
                Payment payment = Payment.builder()
                                .user(user)
                                .amount(registrationFee)
                                .type(PaymentType.AGENT_REGISTRATION)
                                .status(PaymentStatus.PENDING)
                                .description("Agent registration fee")
                                .transactionId(generateTransactionId())
                                .paymentPhone(request.getPaymentPhone())
                                .build();
                paymentRepository.save(payment);
                log.info("Agent registration (legacy) initiated for user {} with code {}", userId, agentCode);
                return AgentRegistrationResponse.builder()
                                .agent(mapToAgentResponse(agent))
                                .orderId(null)
                                .build();
        }

        /**
         * Packages available for new agent registration (same as getAvailablePackages but exposed to non-AGENT users).
         */
        public List<AgentPackageResponse> getRegistrationPackages() {
                return agentPackageRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                                .map(this::mapAgentPackageToResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Get agent profile by user ID
         */
        public AgentResponse getAgentByUserId(UUID userId) {
                Agent agent = agentRepository.findByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("Agent", "userId", userId));
                return mapToAgentResponse(agent);
        }

        /**
         * Get agent by agent code
         */
        public AgentResponse getAgentByCode(String agentCode) {
                Agent agent = agentRepository.findByAgentCode(agentCode)
                                .orElseThrow(() -> new ResourceNotFoundException("Agent", "code", agentCode));
                return mapToAgentResponse(agent);
        }

        /**
         * Agent activates a new business
         * Business pays 10,000/=, Agent gets 5,000/= commission
         */
        @Transactional
        public BusinessResponse activateBusiness(UUID agentUserId, BusinessActivationRequest request) {
                // Get agent
                Agent agent = agentRepository.findByUserId(agentUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));

                // Verify agent is active
                if (agent.getStatus() != AgentStatus.ACTIVE) {
                        throw new BadRequestException("Your agent account is not active. Status: " + agent.getStatus());
                }

                // Agents only register users who DON'T have an account. Users with an account complete payment in the app (USSD) and the system approves them.
                if (request.getOwnerId() != null) {
                        throw new BadRequestException(
                                        "Users with an account must complete business activation in the app (USSD payment). Use this flow only to register new users who do not have an account.");
                }

                // Create new user for business owner (no account)
                if (request.getOwnerPhone() == null || request.getOwnerName() == null) {
                        throw new BadRequestException(
                                        "Owner phone and name are required for new business owner");
                }
                if (request.getOwnerPassword() == null || request.getOwnerPassword().trim().length() < 6) {
                        throw new BadRequestException(
                                        "Owner password is required (min 6 characters) so they can log in after payment.");
                }
                if (userRepository.existsByPhone(request.getOwnerPhone())) {
                        throw new BadRequestException(
                                        "Phone number already registered. That user should request business in the app and pay via USSD.");
                }
                if (request.getOwnerEmail() != null && !request.getOwnerEmail().isBlank()
                                && userRepository.existsByEmail(request.getOwnerEmail().trim())) {
                        throw new BadRequestException("Email already registered.");
                }

                String encodedPassword = passwordEncoder.encode(request.getOwnerPassword().trim());
                User owner = User.builder()
                                .name(request.getOwnerName())
                                .phone(request.getOwnerPhone())
                                .email(request.getOwnerEmail() != null ? request.getOwnerEmail().trim() : null)
                                .password(encodedPassword)
                                .role(Role.BUSINESS)
                                .isVerified(false)
                                .isActive(true)
                                .build();
                owner = userRepository.save(owner);

                // Check if owner already has a business
                if (businessRepository.findByOwnerId(owner.getId()).isPresent()) {
                        throw new BadRequestException("This user already has a registered business");
                }

                // Create business
                Business business = Business.builder()
                                .name(request.getBusinessName())
                                .description(request.getDescription())
                                .category(request.getCategory())
                                .owner(owner)
                                .agent(agent)
                                .region(request.getRegion())
                                .district(request.getDistrict())
                                .ward(request.getWard())
                                .street(request.getStreet())
                                .latitude(request.getLatitude())
                                .longitude(request.getLongitude())
                                .status(BusinessStatus.PENDING) // Pending until payment confirmed
                                .isVerified(false)
                                .build();

                business = businessRepository.save(business);

                BigDecimal activationFee = systemSettingsService.getToBeBusinessAmount();
                Payment payment = Payment.builder()
                                .user(owner)
                                .amount(activationFee)
                                .type(PaymentType.BUSINESS_ACTIVATION)
                                .status(PaymentStatus.PENDING)
                                .description("Business activation fee for " + request.getBusinessName())
                                .transactionId(generateTransactionId())
                                .paymentPhone(request.getPaymentPhone())
                                .build();

                paymentRepository.save(payment);

                // Update owner role
                owner.setRole(Role.BUSINESS);
                userRepository.save(owner);

                // Increment agent's business count
                agent.incrementBusinessCount();
                agentRepository.save(agent);

                // TODO: Initiate payment via M-Pesa/Tigo Pesa
                log.info("Business {} activated by agent {} for owner {}",
                                business.getName(), agent.getAgentCode(), owner.getId());

                return mapToBusinessResponse(business);
        }

        /**
         * Get businesses activated by an agent
         */
        public PagedResponse<BusinessResponse> getAgentBusinesses(UUID agentUserId, int page, int size) {
                Agent agent = agentRepository.findByUserId(agentUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));

                Pageable pageable = PageRequest.of(page, size);
                Page<Business> businesses = businessRepository.findByAgentId(agent.getId(), pageable);

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

        /**
         * Approve/verify a business activation manually (e.g. after offline payment).
         */
        @Transactional
        public BusinessResponse approveBusiness(UUID businessId, UUID agentUserId) {
                Agent agent = agentRepository.findByUserId(agentUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));
                Business business = businessRepository.findById(businessId)
                                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));
                if (!business.getAgent().getId().equals(agent.getId())) {
                        throw new BadRequestException("You can only approve businesses you activated");
                }
                if (business.getStatus() != BusinessStatus.PENDING) {
                        throw new BadRequestException("Business is not pending approval. Status: " + business.getStatus());
                }
                business.setStatus(BusinessStatus.ACTIVE);
                businessRepository.save(business);

                Commission commission = Commission.builder()
                                .agent(agent)
                                .business(business)
                                .amount(AGENT_COMMISSION)
                                .type(CommissionType.ACTIVATION)
                                .status(CommissionStatus.PENDING)
                                .description("Commission for activating business: " + business.getName())
                                .build();
                commissionRepository.save(commission);
                agent.addEarnings(AGENT_COMMISSION);
                agentRepository.save(agent);

                log.info("Business {} approved by agent {}", business.getName(), agent.getAgentCode());
                return mapToBusinessResponse(business);
        }

        /**
         * Cancel a pending business activation.
         */
        @Transactional
        public void cancelBusiness(UUID businessId, UUID agentUserId) {
                Agent agent = agentRepository.findByUserId(agentUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));
                Business business = businessRepository.findById(businessId)
                                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));
                if (!business.getAgent().getId().equals(agent.getId())) {
                        throw new BadRequestException("You can only cancel businesses you activated");
                }
                if (business.getStatus() != BusinessStatus.PENDING) {
                        throw new BadRequestException("Only pending business activations can be cancelled");
                }
                business.setStatus(BusinessStatus.INACTIVE);
                businessRepository.save(business);
                log.info("Business activation {} cancelled by agent {}", businessId, agent.getAgentCode());
        }

        /**
         * Get agent's commission history
         */
        public PagedResponse<CommissionResponse> getAgentCommissions(UUID agentUserId, int page, int size) {
                Agent agent = agentRepository.findByUserId(agentUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));

                Pageable pageable = PageRequest.of(page, size);
                Page<Commission> commissions = commissionRepository.findByAgentIdOrderByCreatedAtDesc(agent.getId(),
                                pageable);

                return PagedResponse.<CommissionResponse>builder()
                                .content(commissions.getContent().stream()
                                                .map(this::mapToCommissionResponse)
                                                .collect(Collectors.toList()))
                                .page(commissions.getNumber())
                                .size(commissions.getSize())
                                .totalElements(commissions.getTotalElements())
                                .totalPages(commissions.getTotalPages())
                                .last(commissions.isLast())
                                .first(commissions.isFirst())
                                .build();
        }

        /**
         * Search for agents
         */
        public PagedResponse<AgentResponse> searchAgents(String query, int page, int size) {
                Pageable pageable = PageRequest.of(page, size);
                Page<Agent> agents = agentRepository.searchAgents(query, pageable);

                return PagedResponse.<AgentResponse>builder()
                                .content(agents.getContent().stream()
                                                .map(this::mapToAgentResponse)
                                                .collect(Collectors.toList()))
                                .page(agents.getNumber())
                                .size(agents.getSize())
                                .totalElements(agents.getTotalElements())
                                .totalPages(agents.getTotalPages())
                                .last(agents.isLast())
                                .first(agents.isFirst())
                                .build();
        }

        /**
         * List agents for "Become a business" flow. Public/authenticated.
         * sort: popularity (businessesActivated desc), rating (avg rating desc), nearby (distance from lat,lng).
         * For nearby, pass lat and lng (or from user profile).
         */
        @Transactional(readOnly = true)
        public PagedResponse<AgentResponse> getAgentsForBusinessRequest(String sort, Double lat, Double lng, int page, int size) {
                List<Agent> agents;
                long total;
                if ("nearby".equalsIgnoreCase(sort) && lat != null && lng != null) {
                        List<Agent> withLocation = agentRepository.findByStatusAndLatitudeIsNotNullAndLongitudeIsNotNull(AgentStatus.ACTIVE);
                        total = withLocation.size();
                        withLocation.sort(Comparator.comparingDouble(a -> distanceSq(a.getLatitude(), a.getLongitude(), lat, lng)));
                        int from = page * size;
                        int to = Math.min(from + size, withLocation.size());
                        agents = from < withLocation.size() ? withLocation.subList(from, to) : List.of();
                } else if ("rating".equalsIgnoreCase(sort)) {
                        Page<Agent> all = agentRepository.findByStatus(AgentStatus.ACTIVE, PageRequest.of(0, 500));
                        List<Agent> list = all.getContent();
                        List<UUID> ids = list.stream().map(Agent::getId).collect(Collectors.toList());
                        Map<UUID, Double> avgMap = new HashMap<>();
                        Map<UUID, Long> countMap = new HashMap<>();
                        if (!ids.isEmpty()) {
                                for (Object[] row : agentRatingRepository.getAverageAndCountByAgentIds(ids)) {
                                        avgMap.put((UUID) row[0], ((Number) row[1]).doubleValue());
                                        countMap.put((UUID) row[0], ((Number) row[2]).longValue());
                                }
                        }
                        list.sort(Comparator.<Agent>comparingDouble(a -> avgMap.getOrDefault(a.getId(), 0.0)).reversed());
                        total = list.size();
                        int from = page * size;
                        int to = Math.min(from + size, list.size());
                        agents = from < list.size() ? list.subList(from, to) : List.of();
                } else {
                        // popularity (default)
                        Page<Agent> pageResult = agentRepository.findByStatus(AgentStatus.ACTIVE,
                                        PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "businessesActivated")));
                        agents = pageResult.getContent();
                        total = pageResult.getTotalElements();
                }
                List<UUID> agentIds = agents.stream().map(Agent::getId).collect(Collectors.toList());
                Map<UUID, Double> avgMap = new HashMap<>();
                Map<UUID, Long> countMap = new HashMap<>();
                if (!agentIds.isEmpty()) {
                        for (Object[] row : agentRatingRepository.getAverageAndCountByAgentIds(agentIds)) {
                                avgMap.put((UUID) row[0], ((Number) row[1]).doubleValue());
                                countMap.put((UUID) row[0], ((Number) row[2]).longValue());
                        }
                }
                LocalDateTime onlineThreshold = LocalDateTime.now().minus(Duration.ofMinutes(ONLINE_MINUTES));
                List<AgentResponse> content = agents.stream()
                                .map(a -> {
                                        Double avg = avgMap.get(a.getId());
                                        Long cnt = countMap.get(a.getId());
                                        boolean online = a.getUser().getLastSeen() != null && a.getUser().getLastSeen().isAfter(onlineThreshold);
                                        return mapToAgentResponse(a, avg, cnt, online);
                                })
                                .collect(Collectors.toList());
                int totalPages = (int) Math.ceil((double) total / size);
                return PagedResponse.<AgentResponse>builder()
                                .content(content)
                                .page(page)
                                .size(size)
                                .totalElements(total)
                                .totalPages(totalPages)
                                .last(page >= totalPages - 1)
                                .first(page == 0)
                                .build();
        }

        private static double distanceSq(Double aLat, Double aLng, double lat, double lng) {
                if (aLat == null || aLng == null) return Double.MAX_VALUE;
                double dLat = aLat - lat;
                double dLng = aLng - lng;
                return dLat * dLat + dLng * dLng;
        }

        /**
         * Rate the agent who activated your business. Allowed only if current user has a business activated by this agent.
         */
        @Transactional
        public void rateAgent(UUID userId, UUID agentId, int rating, String comment) {
                if (rating < 1 || rating > 5) {
                        throw new BadRequestException("Rating must be between 1 and 5");
                }
                Business business = businessRepository.findByOwnerId(userId)
                                .orElseThrow(() -> new BadRequestException("You do not have a business. Only business owners can rate agents."));
                if (business.getAgent() == null || !business.getAgent().getId().equals(agentId)) {
                        throw new BadRequestException("You can only rate the agent who activated your business.");
                }
                User user = userRepository.findById(userId).orElseThrow();
                Agent agent = agentRepository.findById(agentId)
                                .orElseThrow(() -> new ResourceNotFoundException("Agent", "id", agentId));
                AgentRating existing = agentRatingRepository.findByRaterUserIdAndAgentId(userId, agentId).orElse(null);
                if (existing != null) {
                        existing.setRating(rating);
                        existing.setComment(comment != null ? comment.trim() : null);
                        agentRatingRepository.save(existing);
                        log.info("Agent rating updated by user {} for agent {}", userId, agentId);
                } else {
                        AgentRating ar = AgentRating.builder()
                                        .raterUser(user)
                                        .agent(agent)
                                        .rating(rating)
                                        .comment(comment != null ? comment.trim() : null)
                                        .build();
                        agentRatingRepository.save(ar);
                        log.info("Agent rated by user {} for agent {}: {} stars", userId, agentId, rating);
                }
        }

        /**
         * Confirm payment and activate agent/business
         * Called by payment webhook
         */
        @Transactional
        public void confirmPayment(String transactionId) {
                Payment payment = paymentRepository.findByTransactionId(transactionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionId",
                                                transactionId));

                if (payment.getStatus() == PaymentStatus.SUCCESS) {
                        log.info("Payment {} already confirmed", transactionId);
                        return;
                }

                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);

                if (payment.getType() == PaymentType.AGENT_REGISTRATION) {
                        // Activate agent
                        Agent agent = agentRepository.findByUserId(payment.getUser().getId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Agent not found for payment"));
                        agent.setStatus(AgentStatus.ACTIVE);
                        agent.setApprovedAt(LocalDateTime.now());
                        agentRepository.save(agent);
                        log.info("Agent {} activated after payment {}", agent.getAgentCode(), transactionId);

                } else if (payment.getType() == PaymentType.BUSINESS_ACTIVATION) {
                        // Activate business and create commission for agent
                        Business business = businessRepository.findByOwnerId(payment.getUser().getId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Business not found for payment"));

                        business.setStatus(BusinessStatus.ACTIVE);
                        businessRepository.save(business);

                        // Create subscription (1 month free trial or based on payment)
                        // TODO: Create subscription record

                        // Create commission for agent
                        Commission commission = Commission.builder()
                                        .agent(business.getAgent())
                                        .business(business)
                                        .amount(AGENT_COMMISSION)
                                        .type(CommissionType.ACTIVATION)
                                        .status(CommissionStatus.PENDING)
                                        .description("Commission for activating business: " + business.getName())
                                        .build();
                        commissionRepository.save(commission);

                        // Update agent earnings
                        Agent agent = business.getAgent();
                        agent.addEarnings(AGENT_COMMISSION);
                        agentRepository.save(agent);

                        log.info("Business {} activated, commission {} created for agent {}",
                                        business.getName(), AGENT_COMMISSION, agent.getAgentCode());
                }
        }

        // Helper methods

        private String generateAgentCode() {
                String code;
                do {
                        code = "WKL" + String.format("%06d", new Random().nextInt(999999));
                } while (agentRepository.existsByAgentCode(code));
                return code;
        }

        private String generateTransactionId() {
                return "TXN" + System.currentTimeMillis() + new Random().nextInt(1000);
        }

        private AgentResponse mapToAgentResponse(Agent agent) {
                return mapToAgentResponse(agent, null, null, null);
        }

        private AgentResponse mapToAgentResponse(Agent agent, Double averageRating, Long ratingCount, Boolean isOnline) {
                BigDecimal pendingBalance = commissionRepository.sumPendingByAgentId(agent.getId());
                return AgentResponse.builder()
                                .id(agent.getId())
                                .userId(agent.getUser().getId())
                                .name(agent.getUser().getName())
                                .phone(agent.getUser().getPhone())
                                .email(agent.getUser().getEmail())
                                .profilePic(agent.getUser().getProfilePic())
                                .agentCode(agent.getAgentCode())
                                .nationalId(agent.getNationalId())
                                .status(agent.getStatus())
                                .isVerified(agent.getIsVerified())
                                .region(agent.getRegion())
                                .district(agent.getDistrict())
                                .ward(agent.getWard())
                                .latitude(agent.getLatitude())
                                .longitude(agent.getLongitude())
                                .averageRating(averageRating)
                                .ratingCount(ratingCount != null ? ratingCount : 0L)
                                .isOnline(isOnline)
                                .totalEarnings(agent.getTotalEarnings())
                                .availableBalance(pendingBalance != null ? pendingBalance : BigDecimal.ZERO)
                                .businessesActivated(agent.getBusinessesActivated())
                                .totalReferrals(agent.getTotalReferrals())
                                .registeredAt(agent.getCreatedAt())
                                .approvedAt(agent.getApprovedAt())
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
                                .agentId(business.getAgent().getId())
                                .agentName(business.getAgent().getUser().getName())
                                .agentCode(business.getAgent().getAgentCode())
                                .createdAt(business.getCreatedAt())
                                .build();
        }

        private CommissionResponse mapToCommissionResponse(Commission commission) {
                return CommissionResponse.builder()
                                .id(commission.getId())
                                .type(commission.getType())
                                .amount(commission.getAmount())
                                .status(commission.getStatus())
                                .description(commission.getDescription())
                                .businessId(commission.getBusiness() != null ? commission.getBusiness().getId() : null)
                                .businessName(commission.getBusiness() != null ? commission.getBusiness().getName()
                                                : null)
                                .earnedAt(commission.getCreatedAt())
                                .paidAt(commission.getPaidAt())
                                .build();
        }

        // ==================== WITHDRAWAL METHODS ====================

        private static final BigDecimal MINIMUM_WITHDRAWAL = new BigDecimal("5000.00");

        /**
         * Request a withdrawal
         */
        @Transactional
        public WithdrawalResponse requestWithdrawal(UUID agentUserId, WithdrawalRequest request) {
                Agent agent = agentRepository.findByUserId(agentUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));

                if (agent.getStatus() != AgentStatus.ACTIVE) {
                        throw new BadRequestException("Your agent account is not active");
                }

                // Check if there's a pending withdrawal
                if (withdrawalRepository.existsByAgentIdAndStatus(agent.getId(), WithdrawalStatus.PENDING)) {
                        throw new BadRequestException(
                                        "You have a pending withdrawal request. Please wait for it to be processed.");
                }

                // Check minimum withdrawal
                if (request.getAmount().compareTo(MINIMUM_WITHDRAWAL) < 0) {
                        throw new BadRequestException("Minimum withdrawal amount is " + MINIMUM_WITHDRAWAL + " TZS");
                }

                // Check available balance
                if (agent.getAvailableBalance().compareTo(request.getAmount()) < 0) {
                        throw new BadRequestException(
                                        "Insufficient balance. Available: " + agent.getAvailableBalance() + " TZS");
                }

                // Create withdrawal request
                Withdrawal withdrawal = Withdrawal.builder()
                                .agent(agent)
                                .amount(request.getAmount())
                                .paymentMethod(request.getPaymentMethod())
                                .paymentPhone(request.getPaymentPhone())
                                .paymentName(request.getPaymentName())
                                .notes(request.getNotes())
                                .status(WithdrawalStatus.PENDING)
                                .build();

                withdrawal = withdrawalRepository.save(withdrawal);

                // Deduct from available balance (hold until processed)
                agent.setAvailableBalance(agent.getAvailableBalance().subtract(request.getAmount()));
                agentRepository.save(agent);

                log.info("Withdrawal request {} for {} TZS created by agent {}",
                                withdrawal.getId(), request.getAmount(), agent.getAgentCode());

                return mapToWithdrawalResponse(withdrawal);
        }

        /**
         * Get agent's withdrawal history
         */
        public PagedResponse<WithdrawalResponse> getWithdrawalHistory(UUID agentUserId, int page, int size) {
                Agent agent = agentRepository.findByUserId(agentUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));

                Pageable pageable = PageRequest.of(page, size);
                Page<Withdrawal> withdrawals = withdrawalRepository.findByAgentIdOrderByCreatedAtDesc(agent.getId(),
                                pageable);

                return PagedResponse.<WithdrawalResponse>builder()
                                .content(withdrawals.getContent().stream()
                                                .map(this::mapToWithdrawalResponse)
                                                .collect(Collectors.toList()))
                                .page(withdrawals.getNumber())
                                .size(withdrawals.getSize())
                                .totalElements(withdrawals.getTotalElements())
                                .totalPages(withdrawals.getTotalPages())
                                .last(withdrawals.isLast())
                                .first(withdrawals.isFirst())
                                .build();
        }

        /**
         * Cancel a pending withdrawal
         */
        @Transactional
        public void cancelWithdrawal(UUID withdrawalId, UUID agentUserId) {
                Agent agent = agentRepository.findByUserId(agentUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));

                Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal", "id", withdrawalId));

                if (!withdrawal.getAgent().getId().equals(agent.getId())) {
                        throw new ResourceNotFoundException("Withdrawal", "id", withdrawalId);
                }

                if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
                        throw new BadRequestException("Only pending withdrawals can be cancelled");
                }

                // Return amount to available balance
                agent.setAvailableBalance(agent.getAvailableBalance().add(withdrawal.getAmount()));
                agentRepository.save(agent);

                // Delete withdrawal
                withdrawalRepository.delete(withdrawal);

                log.info("Withdrawal {} cancelled by agent {}", withdrawalId, agent.getAgentCode());
        }

        private WithdrawalResponse mapToWithdrawalResponse(Withdrawal withdrawal) {
                return WithdrawalResponse.builder()
                                .id(withdrawal.getId())
                                .amount(withdrawal.getAmount())
                                .status(withdrawal.getStatus())
                                .paymentMethod(withdrawal.getPaymentMethod())
                                .paymentPhone(withdrawal.getPaymentPhone())
                                .paymentName(withdrawal.getPaymentName())
                                .transactionId(withdrawal.getTransactionId())
                                .rejectionReason(withdrawal.getRejectionReason())
                                .processedAt(withdrawal.getProcessedAt())
                                .createdAt(withdrawal.getCreatedAt())
                                .agent(WithdrawalResponse.AgentSummary.builder()
                                                .id(withdrawal.getAgent().getId())
                                                .agentCode(withdrawal.getAgent().getAgentCode())
                                                .name(withdrawal.getAgent().getUser().getName())
                                                .build())
                                .build();
        }

        /**
         * Get Agent Dashboard Summary
         * Provides key stats for agent's wallet and performance
         */
        @Transactional(readOnly = true)
        public AgentDashboardResponse getAgentDashboard(UUID agentUserId) {
                Agent agent = agentRepository.findByUserId(agentUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Agent", "userId", agentUserId.toString()));

                // Get today's start time for today's earnings calculation
                LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();

                BigDecimal todayEarnings = commissionRepository.sumEarningsAfter(agent.getId(), startOfToday);
                BigDecimal pendingWithdrawals = withdrawalRepository.sumAmountByAgentIdAndStatus(
                                agent.getId(), WithdrawalStatus.PENDING);

                AgentDashboardResponse.AgentDashboardResponseBuilder b = AgentDashboardResponse.builder()
                                .currentBalance(agent.getAvailableBalance())
                                .totalEarnings(agent.getTotalEarnings())
                                .todayEarnings(todayEarnings != null ? todayEarnings : BigDecimal.ZERO)
                                .pendingWithdrawals(pendingWithdrawals != null ? pendingWithdrawals : BigDecimal.ZERO)
                                .totalBusinessesActivated(agent.getBusinessesActivated())
                                .totalReferrals(agent.getTotalReferrals());

                if (agent.getAgentPackage() != null) {
                        AgentPackage pkg = agent.getAgentPackage();
                        Integer maxBiz = pkg.getNumberOfBusinesses();
                        Integer activated = agent.getBusinessesActivated();
                        b.packageId(pkg.getId())
                                        .packageName(pkg.getName())
                                        .packageMaxBusinesses(maxBiz)
                                        .packageRemainingBusinesses(maxBiz != null && activated != null ? Math.max(0, maxBiz - activated) : null);
                }
                return b.build();
        }

        /**
         * Get all available (active) agent packages for purchase/upgrade.
         */
        public List<AgentPackageResponse> getAvailablePackages() {
                return agentPackageRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                                .map(this::mapAgentPackageToResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Initiate payment for agent package purchase/upgrade.
         * Returns order/transaction ID for USSD follow-up.
         */
        @Transactional
        public String initiatePackagePurchase(UUID userId, UUID packageId, String paymentPhone) {
                AgentPackage agentPackage = agentPackageRepository.findById(packageId)
                                .orElseThrow(() -> new ResourceNotFoundException("Agent Package", "id", packageId));
                if (!Boolean.TRUE.equals(agentPackage.getIsActive())) {
                        throw new BadRequestException("This package is not available for purchase");
                }
                String description = "Agent package: " + agentPackage.getName();
                return paymentService.initiatePayment(
                                userId,
                                agentPackage.getPrice(),
                                PaymentType.AGENT_PACKAGE,
                                paymentPhone,
                                description,
                                packageId,
                                "AGENT_PACKAGE");
        }

        private AgentPackageResponse mapAgentPackageToResponse(AgentPackage agentPackage) {
                return AgentPackageResponse.builder()
                                .id(agentPackage.getId())
                                .name(agentPackage.getName())
                                .description(agentPackage.getDescription())
                                .price(agentPackage.getPrice())
                                .numberOfBusinesses(agentPackage.getNumberOfBusinesses())
                                .isActive(agentPackage.getIsActive())
                                .isPopular(agentPackage.getIsPopular())
                                .sortOrder(agentPackage.getSortOrder())
                                .createdAt(agentPackage.getCreatedAt())
                                .updatedAt(agentPackage.getUpdatedAt())
                                .build();
        }
}
