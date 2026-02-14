package com.wakilfly.service;

import com.wakilfly.dto.response.*;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.*;
import com.wakilfly.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final AgentRepository agentRepository;
    private final OrderRepository orderRepository;
    private final PostRepository postRepository;
    private final ProductRepository productRepository;
    private final ReportRepository reportRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final PromotionRepository promotionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogService auditLogService;
    private final AgentPackageRepository agentPackageRepository;

    /**
     * Get dashboard statistics
     */
    public AdminDashboardResponse getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.with(LocalTime.MIN);
        LocalDateTime startOfWeek = now.minusDays(7);
        LocalDateTime startOfMonth = now.minusDays(30);

        return AdminDashboardResponse.builder()
                // User stats
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByIsActiveTrue())
                .newUsersToday(userRepository.countByCreatedAtAfter(startOfDay))
                .newUsersThisWeek(userRepository.countByCreatedAtAfter(startOfWeek))
                .newUsersThisMonth(userRepository.countByCreatedAtAfter(startOfMonth))

                // Business stats
                .totalBusinesses(businessRepository.count())
                .activeBusinesses(businessRepository.countByStatus(BusinessStatus.ACTIVE))
                .pendingBusinesses(businessRepository.countByStatus(BusinessStatus.PENDING))

                // Agent stats
                .totalAgents(agentRepository.count())
                .activeAgents(agentRepository.countByStatus(AgentStatus.ACTIVE))
                .pendingAgents(agentRepository.countByStatus(AgentStatus.PENDING))

                // Order stats
                .totalOrders(orderRepository.count())
                .pendingOrders(orderRepository.countByStatus(OrderStatus.PENDING))
                .completedOrders(orderRepository.countByStatus(OrderStatus.COMPLETED))
                .cancelledOrders(orderRepository.countByStatus(OrderStatus.CANCELLED))

                // Revenue stats
                .totalRevenue(paymentRepository.sumByStatus(PaymentStatus.SUCCESS))
                .revenueToday(paymentRepository.sumByStatusAndDateAfter(PaymentStatus.SUCCESS, startOfDay))
                .revenueThisWeek(paymentRepository.sumByStatusAndDateAfter(PaymentStatus.SUCCESS, startOfWeek))
                .revenueThisMonth(paymentRepository.sumByStatusAndDateAfter(PaymentStatus.SUCCESS, startOfMonth))

                // Content stats
                .totalPosts(postRepository.countByIsDeletedFalse())
                .totalProducts(productRepository.countByIsActiveTrue())
                .totalPromotions(promotionRepository.countByStatus(PromotionStatus.ACTIVE))

                // Moderation stats
                .pendingReports(reportRepository.countByStatus(ReportStatus.PENDING))
                .pendingWithdrawals((long) withdrawalRepository.findByStatus(WithdrawalStatus.PENDING).size())

                // Subscription stats
                .activeSubscriptions(subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE))
                .expiringSubscriptions(subscriptionRepository.countByStatusAndEndDateBefore(
                        SubscriptionStatus.ACTIVE, now.plusDays(7)))

                // Breakdowns
                .usersByRole(getUsersByRole())
                .ordersByStatus(getOrdersByStatus())
                .build();
    }

    /**
     * Get all users (paginated)
     */
    public PagedResponse<UserResponse> getAllUsers(int page, int size, String role, Boolean isActive) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users;

        if (role != null && isActive != null) {
            users = userRepository.findByRoleAndIsActive(Role.valueOf(role), isActive, pageable);
        } else if (role != null) {
            users = userRepository.findByRole(Role.valueOf(role), pageable);
        } else if (isActive != null) {
            users = userRepository.findByIsActive(isActive, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        return PagedResponse.<UserResponse>builder()
                .content(users.getContent().stream()
                        .map(this::mapUserToResponse)
                        .collect(Collectors.toList()))
                .page(users.getNumber())
                .size(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .last(users.isLast())
                .first(users.isFirst())
                .build();
    }

    /**
     * Update user status
     */
    @Transactional
    public UserResponse updateUserStatus(UUID userId, UUID adminId, boolean isActive, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        boolean oldStatus = user.getIsActive();
        user.setIsActive(isActive);
        user = userRepository.save(user);

        auditLogService.log(adminId, isActive ? "USER_ACTIVATED" : "USER_DEACTIVATED",
                "User", userId, reason, String.valueOf(oldStatus), String.valueOf(isActive), null);

        log.info("Admin {} {} user {}: {}", adminId, isActive ? "activated" : "deactivated", userId, reason);

        return mapUserToResponse(user);
    }

    /**
     * Update user role
     */
    @Transactional
    public UserResponse updateUserRole(UUID userId, UUID adminId, Role newRole, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role oldRole = user.getRole();
        user.setRole(newRole);
        user = userRepository.save(user);

        auditLogService.log(adminId, "USER_ROLE_CHANGED", "User", userId, reason,
                oldRole.name(), newRole.name(), null);

        log.info("Admin {} changed role of user {} from {} to {}", adminId, userId, oldRole, newRole);

        return mapUserToResponse(user);
    }

    /**
     * Get all businesses (paginated)
     */
    public PagedResponse<BusinessResponse> getAllBusinesses(int page, int size, BusinessStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Business> businesses;

        if (status != null) {
            businesses = businessRepository.findByStatus(status, pageable);
        } else {
            businesses = businessRepository.findAll(pageable);
        }

        return PagedResponse.<BusinessResponse>builder()
                .content(businesses.getContent().stream()
                        .map(this::mapBusinessToResponse)
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
     * Update business status
     */
    @Transactional
    public BusinessResponse updateBusinessStatus(UUID businessId, UUID adminId, BusinessStatus status, String reason) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));

        BusinessStatus oldStatus = business.getStatus();
        business.setStatus(status);
        business = businessRepository.save(business);

        auditLogService.log(adminId, "BUSINESS_STATUS_CHANGED", "Business", businessId, reason,
                oldStatus.name(), status.name(), null);

        log.info("Admin {} changed business {} status from {} to {}", adminId, businessId, oldStatus, status);

        return mapBusinessToResponse(business);
    }

    /**
     * Verify business
     */
    @Transactional
    public BusinessResponse verifyBusiness(UUID businessId, UUID adminId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));

        business.setIsVerified(true);
        business = businessRepository.save(business);

        auditLogService.log(adminId, "BUSINESS_VERIFIED", "Business", businessId,
                "Business verified by admin", null, null, null);

        return mapBusinessToResponse(business);
    }

    /**
     * Verify user (Blue Tick)
     */
    @Transactional
    public Map<String, Object> verifyUser(UUID userId, UUID adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setIsVerified(true);
        user = userRepository.save(user);

        auditLogService.log(adminId, "USER_VERIFIED", "User", userId,
                "User verified (Blue Tick) by admin", null, null, null);

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("isVerified", user.getIsVerified());
        return response;
    }

    /**
     * Get pending withdrawals
     */
    public PagedResponse<WithdrawalResponse> getPendingWithdrawals(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Withdrawal> withdrawals = withdrawalRepository.findByStatusOrderByCreatedAtDesc(WithdrawalStatus.PENDING,
                pageable);

        return PagedResponse.<WithdrawalResponse>builder()
                .content(withdrawals.getContent().stream()
                        .map(this::mapWithdrawalToResponse)
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
     * Process a withdrawal
     */
    @Transactional
    public WithdrawalResponse processWithdrawal(UUID withdrawalId, UUID adminId, boolean approve, String notes,
            String transactionId) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal", "id", withdrawalId));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new BadRequestException("Withdrawal is not pending");
        }

        if (approve) {
            withdrawal.setStatus(WithdrawalStatus.COMPLETED);
            withdrawal.setTransactionId(transactionId);
            withdrawal.setProcessedAt(LocalDateTime.now());

            // Update agent paid earnings
            Agent agent = withdrawal.getAgent();
            agent.setTotalEarnings(agent.getTotalEarnings().subtract(withdrawal.getAmount()));
            agentRepository.save(agent);

            auditLogService.log(adminId, "WITHDRAWAL_APPROVED", "Withdrawal", withdrawalId,
                    "Amount: " + withdrawal.getAmount() + " TZS, TxnID: " + transactionId, null, null, null);
        } else {
            withdrawal.setStatus(WithdrawalStatus.REJECTED);
            withdrawal.setRejectionReason(notes);
            withdrawal.setProcessedAt(LocalDateTime.now());

            // Return amount to agent's available balance
            Agent agent = withdrawal.getAgent();
            agent.setAvailableBalance(agent.getAvailableBalance().add(withdrawal.getAmount()));
            agentRepository.save(agent);

            auditLogService.log(adminId, "WITHDRAWAL_REJECTED", "Withdrawal", withdrawalId,
                    "Reason: " + notes, null, null, null);
        }

        withdrawal = withdrawalRepository.save(withdrawal);

        log.info("Admin {} {} withdrawal {}", adminId, approve ? "approved" : "rejected", withdrawalId);

        return mapWithdrawalToResponse(withdrawal);
    }

    /**
     * Get all agents
     */
    public PagedResponse<AgentResponse> getAllAgents(int page, int size, AgentStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Agent> agents;

        if (status != null) {
            agents = agentRepository.findByStatus(status, pageable);
        } else {
            agents = agentRepository.findAll(pageable);
        }

        return PagedResponse.<AgentResponse>builder()
                .content(agents.getContent().stream()
                        .map(this::mapAgentToResponse)
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
     * Update agent status
     */
    @Transactional
    public AgentResponse updateAgentStatus(UUID agentId, UUID adminId, AgentStatus status, String reason) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "id", agentId));

        AgentStatus oldStatus = agent.getStatus();
        agent.setStatus(status);

        if (status == AgentStatus.ACTIVE && oldStatus == AgentStatus.PENDING) {
            agent.setApprovedAt(LocalDateTime.now());
        }

        agent = agentRepository.save(agent);

        auditLogService.log(adminId, "AGENT_STATUS_CHANGED", "Agent", agentId, reason,
                oldStatus.name(), status.name(), null);

        log.info("Admin {} changed agent {} status from {} to {}", adminId, agentId, oldStatus, status);

        return mapAgentToResponse(agent);
    }

    /**
     * Verify agent
     */
    @Transactional
    public AgentResponse verifyAgent(UUID agentId, UUID adminId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "id", agentId));

        agent.setIsVerified(true);
        agent = agentRepository.save(agent);

        auditLogService.log(adminId, "AGENT_VERIFIED", "Agent", agentId,
                "Agent verified by admin", null, null, null);

        return mapAgentToResponse(agent);
    }

    // Helper methods

    private Map<String, Long> getUsersByRole() {
        Map<String, Long> result = new HashMap<>();
        for (Role role : Role.values()) {
            result.put(role.name(), userRepository.countByRole(role));
        }
        return result;
    }

    private Map<String, Long> getOrdersByStatus() {
        Map<String, Long> result = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            result.put(status.name(), orderRepository.countByStatus(status));
        }
        return result;
    }

    private UserResponse mapUserToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profilePic(user.getProfilePic())
                .bio(user.getBio())
                .role(user.getRole())
                .isVerified(user.getIsVerified())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private BusinessResponse mapBusinessToResponse(Business business) {
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
                .createdAt(business.getCreatedAt())
                .build();
    }

    private WithdrawalResponse mapWithdrawalToResponse(Withdrawal withdrawal) {
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

    private AgentResponse mapAgentToResponse(Agent agent) {
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
                .totalEarnings(agent.getTotalEarnings())
                .availableBalance(agent.getAvailableBalance())
                .businessesActivated(agent.getBusinessesActivated())
                .totalReferrals(agent.getTotalReferrals())
                .registeredAt(agent.getCreatedAt())
                .approvedAt(agent.getApprovedAt())
                .build();
    }

    // ==================== AGENT PACKAGE MANAGEMENT ====================

    /**
     * Get all agent packages
     */
    public java.util.List<AgentPackageResponse> getAllAgentPackages() {
        return agentPackageRepository.findAll().stream()
                .map(this::mapAgentPackageToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create agent package
     */
    @Transactional
    public AgentPackageResponse createAgentPackage(com.wakilfly.dto.request.CreateAgentPackageRequest request) {
        AgentPackage agentPackage = AgentPackage.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .numberOfBusinesses(request.getNumberOfBusinesses())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isPopular(request.getIsPopular() != null ? request.getIsPopular() : false)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        agentPackage = agentPackageRepository.save(agentPackage);
        return mapAgentPackageToResponse(agentPackage);
    }

    /**
     * Update agent package
     */
    @Transactional
    public AgentPackageResponse updateAgentPackage(UUID packageId, com.wakilfly.dto.request.CreateAgentPackageRequest request) {
        AgentPackage agentPackage = agentPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("AgentPackage", "id", packageId));

        agentPackage.setName(request.getName());
        if (request.getDescription() != null) agentPackage.setDescription(request.getDescription());
        agentPackage.setPrice(request.getPrice());
        agentPackage.setNumberOfBusinesses(request.getNumberOfBusinesses());
        if (request.getIsActive() != null) agentPackage.setIsActive(request.getIsActive());
        if (request.getIsPopular() != null) agentPackage.setIsPopular(request.getIsPopular());
        if (request.getSortOrder() != null) agentPackage.setSortOrder(request.getSortOrder());

        agentPackage = agentPackageRepository.save(agentPackage);
        return mapAgentPackageToResponse(agentPackage);
    }

    /**
     * Delete agent package
     */
    @Transactional
    public void deleteAgentPackage(UUID packageId) {
        AgentPackage agentPackage = agentPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("AgentPackage", "id", packageId));
        agentPackageRepository.delete(agentPackage);
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
