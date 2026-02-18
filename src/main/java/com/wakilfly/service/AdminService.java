package com.wakilfly.service;

import com.wakilfly.dto.request.AdminCreateUserRequest;
import com.wakilfly.dto.request.AdminSettingsUpdateRequest;
import com.wakilfly.dto.request.CreateAgentPackageRequest;
import com.wakilfly.dto.request.UpdateOrderStatusRequest;
import com.wakilfly.dto.response.*;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.*;
import com.wakilfly.repository.*;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.util.ContinentHelper;
import com.wakilfly.util.ReverseGeocodeUtil;
import com.wakilfly.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
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
    private final AgentPackageRepository agentPackageRepository;
    private final PostMediaRepository postMediaRepository;
    private final AuditLogService auditLogService;
    private final SystemSettingsService systemSettingsService;
    private final OrderService orderService;
    private final ProductService productService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final AdminRoleDefinitionService roleDefinitionService;
    private final PromotionService promotionService;
    private final PostReactionRepository postReactionRepository;
    private final CommentRepository commentRepository;
    private final CommunityMemberRepository communityMemberRepository;

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
     * Get admin settings (agent register amount, to-be-business amount).
     */
    public AdminSettingsResponse getSettings() {
        return systemSettingsService.getSettings();
    }

    /**
     * Update admin settings.
     */
    @Transactional
    public AdminSettingsResponse updateSettings(AdminSettingsUpdateRequest request) {
        return systemSettingsService.updateSettings(request);
    }

    /**
     * Get full user details for admin (profile, location, engagements, communities).
     */
    public AdminUserDetailResponse getUserDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        long followersCount = userRepository.countFollowers(userId);
        long followingCount = userRepository.countFollowing(userId);
        long postsCount = postRepository.countByAuthorIdAndIsDeletedFalse(userId);

        List<AdminUserDetailResponse.CommunityMembership> memberships = communityMemberRepository
                .findByUserIdOrderByJoinedAtDesc(userId)
                .stream()
                .map(cm -> {
                    com.wakilfly.model.Community c = cm.getCommunity();
                    return AdminUserDetailResponse.CommunityMembership.builder()
                            .communityId(c.getId())
                            .name(c.getName())
                            .type(c.getType())
                            .memberRole(cm.getRole())
                            .membersCount(c.getMembersCount() != null ? c.getMembersCount() : 0)
                            .joinedAt(cm.getJoinedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return AdminUserDetailResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .bio(user.getBio())
                .profilePic(user.getProfilePic())
                .coverPic(user.getCoverPic())
                .role(user.getRole())
                .adminRole(user.getRole() == Role.ADMIN ? user.getAdminRoleCode() : null)
                .isVerified(user.getIsVerified())
                .isActive(user.getIsActive())
                .onboardingCompleted(user.getOnboardingCompleted())
                .createdAt(user.getCreatedAt())
                .lastSeen(user.getLastSeen())
                .work(user.getWork())
                .education(user.getEducation())
                .currentCity(user.getCurrentCity())
                .region(user.getRegion())
                .country(user.getCountry())
                .hometown(user.getHometown())
                .gender(user.getGender())
                .language(user.getLanguage())
                .interests(user.getInterests())
                .followersCount(followersCount)
                .followingCount(followingCount)
                .postsCount(postsCount)
                .communities(memberships)
                .build();
    }

    /**
     * Get all users (paginated) with optional search by name, email, or phone.
     */
    public PagedResponse<UserResponse> getAllUsers(int page, int size, String role, Boolean isActive, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users;

        if (search != null && !search.trim().isEmpty()) {
            users = userRepository.searchUsersForAdmin(search.trim(), pageable);
        } else if (role != null && isActive != null) {
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
     * Create a new user (Super Admin only). No OTP; user can login immediately.
     */
    @Transactional
    public UserResponse createUser(UUID adminId, AdminCreateUserRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone number already registered");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank() && userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        Role role = Role.valueOf(request.getRole());
        String adminRoleCode = null;
        if (role == Role.ADMIN && request.getAdminRole() != null && !request.getAdminRole().isBlank()) {
            adminRoleCode = request.getAdminRole().trim().toUpperCase();
            if (!roleDefinitionService.isValidRoleCode(adminRoleCode)) {
                throw new BadRequestException("Invalid admin role: " + adminRoleCode);
            }
        }
        if (role == Role.ADMIN && adminRoleCode == null) {
            adminRoleCode = "SUPER_ADMIN";
        }

        User user = User.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail() : null)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .adminRoleCode(role == Role.ADMIN ? adminRoleCode : null)
                .isVerified(true)
                .isActive(true)
                .onboardingCompleted(true)
                .build();
        user = userRepository.save(user);

        auditLogService.log(adminId, "USER_CREATED", "User", user.getId(),
                "Admin created user with role " + role.name(), null, null, null);
        log.info("Admin {} created user {} with role {}", adminId, user.getId(), role);
        return mapUserToResponse(user);
    }

    /**
     * Set admin sub-role (SUPER_ADMIN only). Target must be an ADMIN user.
     */
    @Transactional
    public UserResponse setUserAdminRole(UUID userId, UUID adminId, String adminRoleCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Target user must have role ADMIN to assign admin role");
        }
        if (adminRoleCode != null && !adminRoleCode.isBlank() && !roleDefinitionService.isValidRoleCode(adminRoleCode)) {
            throw new BadRequestException("Invalid admin role: " + adminRoleCode);
        }
        String oldCode = user.getAdminRoleCode();
        user.setAdminRoleCode(adminRoleCode != null && !adminRoleCode.isBlank() ? adminRoleCode.trim().toUpperCase() : "SUPER_ADMIN");
        user = userRepository.save(user);
        auditLogService.log(adminId, "ADMIN_ROLE_CHANGED", "User", userId, null,
                oldCode != null ? oldCode : "SUPER_ADMIN",
                user.getAdminRoleCode(),
                null);
        log.info("Admin {} set admin role of user {} to {}", adminId, userId, user.getAdminRoleCode());
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
                .adminRole(user.getRole() == Role.ADMIN ? user.getAdminRoleCode() : null)
                .isVerified(user.getIsVerified())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .country(user.getCountry())
                .region(user.getRegion())
                .currentCity(user.getCurrentCity())
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
                .latitude(business.getLatitude())
                .longitude(business.getLongitude())
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

    // ==================== PAYMENT MONITORING ====================

    /**
     * Get all payments with filters (for admin monitoring)
     */
    public PagedResponse<PaymentHistoryResponse> getAllPayments(
            int page, int size,
            PaymentStatus status,
            PaymentType type,
            UUID userId,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Payment> payments = paymentRepository.findAllWithFilters(
                status, type, userId, startDate, endDate, pageable);

        return PagedResponse.<PaymentHistoryResponse>builder()
                .content(payments.getContent().stream()
                        .map(this::mapPaymentToHistoryResponse)
                        .collect(Collectors.toList()))
                .page(payments.getNumber())
                .size(payments.getSize())
                .totalElements(payments.getTotalElements())
                .totalPages(payments.getTotalPages())
                .last(payments.isLast())
                .first(payments.isFirst())
                .build();
    }

    private PaymentHistoryResponse mapPaymentToHistoryResponse(Payment payment) {
        return PaymentHistoryResponse.builder()
                .id(payment.getId())
                .userId(payment.getUser() != null ? payment.getUser().getId() : null)
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .type(payment.getType())
                .status(payment.getStatus())
                .method(payment.getMethod())
                .paymentPhone(payment.getPaymentPhone())
                .description(payment.getDescription())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
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
     * Create a new agent package
     */
    @Transactional
    public AgentPackageResponse createAgentPackage(CreateAgentPackageRequest request) {
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
        log.info("Agent package created: {}", agentPackage.getId());

        return mapAgentPackageToResponse(agentPackage);
    }

    /**
     * Update an agent package
     */
    @Transactional
    public AgentPackageResponse updateAgentPackage(UUID packageId, CreateAgentPackageRequest request) {
        AgentPackage agentPackage = agentPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent Package", "id", packageId));

        if (request.getName() != null) {
            agentPackage.setName(request.getName());
        }
        if (request.getDescription() != null) {
            agentPackage.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            agentPackage.setPrice(request.getPrice());
        }
        if (request.getNumberOfBusinesses() != null) {
            agentPackage.setNumberOfBusinesses(request.getNumberOfBusinesses());
        }
        if (request.getIsActive() != null) {
            agentPackage.setIsActive(request.getIsActive());
        }
        if (request.getIsPopular() != null) {
            agentPackage.setIsPopular(request.getIsPopular());
        }
        if (request.getSortOrder() != null) {
            agentPackage.setSortOrder(request.getSortOrder());
        }

        agentPackage = agentPackageRepository.save(agentPackage);
        log.info("Agent package updated: {}", agentPackage.getId());

        return mapAgentPackageToResponse(agentPackage);
    }

    /**
     * Delete an agent package
     */
    @Transactional
    public void deleteAgentPackage(UUID packageId) {
        AgentPackage agentPackage = agentPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent Package", "id", packageId));

        // Check if any agents are using this package
        long agentsUsingPackage = agentRepository.findAll().stream()
                .filter(agent -> agent.getAgentPackage() != null && agent.getAgentPackage().getId().equals(packageId))
                .count();
        if (agentsUsingPackage > 0) {
            throw new BadRequestException(
                    String.format("Cannot delete package. %d agent(s) are currently using this package.", agentsUsingPackage));
        }

        agentPackageRepository.delete(agentPackage);
        log.info("Agent package deleted: {}", packageId);
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

    // ==================== CHARTS ====================

    public ChartDataResponse getChartData(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(days);
        List<ChartDataResponse.DayData> revenueByDay = new ArrayList<>();
        List<ChartDataResponse.DayData> usersByDay = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = d.atStartOfDay();
            LocalDateTime dayEnd = d.atTime(LocalTime.MAX);

            BigDecimal rev = paymentRepository.sumByStatusAndDateBetween(PaymentStatus.SUCCESS, dayStart, dayEnd);
            long userCount = userRepository.countByCreatedAtAfter(dayStart);

            revenueByDay.add(ChartDataResponse.DayData.builder()
                    .date(d.toString())
                    .value(rev != null ? rev : BigDecimal.ZERO)
                    .build());
            usersByDay.add(ChartDataResponse.DayData.builder()
                    .date(d.toString())
                    .count(userCount)
                    .build());
        }

        return ChartDataResponse.builder()
                .revenueByDay(revenueByDay)
                .usersByDay(usersByDay)
                .build();
    }

    // ==================== MAP LOCATIONS ====================

    /**
     * All map pins: users, agents, businesses that have coordinates (from registration/profile).
     * Frontend uses type to show icon: USER, AGENT, BUSINESS.
     */
    public List<MapLocationResponse> getMapLocations() {
        List<MapLocationResponse> out = new ArrayList<>();

        // Users (location from registration or profile)
        List<User> users = userRepository.findAllWithCoordinates();
        for (User u : users) {
            String country = u.getCountry() != null ? u.getCountry().trim() : null;
            String region = u.getRegion() != null ? u.getRegion().trim() : null;
            if (country == null && u.getLatitude() != null && u.getLongitude() != null) {
                var geo = ReverseGeocodeUtil.geocode(u.getLatitude(), u.getLongitude());
                if (geo != null) {
                    country = geo.country;
                    if (region == null && geo.region != null) region = geo.region;
                }
            }
            String continent = ContinentHelper.getContinent(country);
            out.add(MapLocationResponse.builder()
                    .id(u.getId())
                    .name(u.getName())
                    .latitude(u.getLatitude())
                    .longitude(u.getLongitude())
                    .type("USER")
                    .region(region)
                    .category(null)
                    .country(country)
                    .continent(continent)
                    .build());
        }

        // Agents (location from agent registration)
        List<Agent> agents = agentRepository.findAllWithCoordinates();
        for (Agent a : agents) {
            String country = null;
            String region = a.getRegion() != null ? a.getRegion().trim() : null;
            if (a.getUser() != null && a.getUser().getCountry() != null) {
                country = a.getUser().getCountry().trim();
            }
            if (country == null && a.getLatitude() != null && a.getLongitude() != null) {
                var geo = ReverseGeocodeUtil.geocode(a.getLatitude(), a.getLongitude());
                if (geo != null) {
                    country = geo.country;
                    if (region == null && geo.region != null) region = geo.region;
                }
            }
            String continent = ContinentHelper.getContinent(country);
            out.add(MapLocationResponse.builder()
                    .id(a.getId())
                    .name(a.getUser() != null ? a.getUser().getName() : "Agent")
                    .latitude(a.getLatitude())
                    .longitude(a.getLongitude())
                    .type("AGENT")
                    .region(region)
                    .category(a.getAgentCode())
                    .country(country)
                    .continent(continent)
                    .build());
        }

        // Businesses (location from business registration)
        List<Business> businesses = businessRepository.findAllWithCoordinates();
        for (Business b : businesses) {
            if (b.getLatitude() != null && b.getLongitude() != null) {
                String country = null;
                String region = b.getRegion() != null ? b.getRegion().trim() : null;
                if (b.getOwner() != null && b.getOwner().getCountry() != null) {
                    country = b.getOwner().getCountry().trim();
                }
                if (country == null) {
                    var geo = ReverseGeocodeUtil.geocode(b.getLatitude(), b.getLongitude());
                    if (geo != null) {
                        country = geo.country;
                        if (region == null && geo.region != null) region = geo.region;
                    }
                }
                String continent = ContinentHelper.getContinent(country);
                out.add(MapLocationResponse.builder()
                        .id(b.getId())
                        .name(b.getName())
                        .latitude(b.getLatitude())
                        .longitude(b.getLongitude())
                        .type("BUSINESS")
                        .region(region)
                        .category(b.getCategory())
                        .country(country)
                        .continent(continent)
                        .build());
            }
        }

        return out;
    }

    /**
     * Aggregated map stats: by continent, by country, by type (USER/AGENT/BUSINESS).
     */
    public MapStatsResponse getMapStats() {
        List<MapLocationResponse> locations = getMapLocations();

        Map<String, long[]> byContinent = new LinkedHashMap<>();
        Map<String, long[]> byCountry = new LinkedHashMap<>();
        Map<String, String> countryToContinent = new LinkedHashMap<>();
        Map<String, Long> byType = new LinkedHashMap<>();
        byType.put("USER", 0L);
        byType.put("AGENT", 0L);
        byType.put("BUSINESS", 0L);

        for (MapLocationResponse loc : locations) {
            String type = loc.getType() != null ? loc.getType().toUpperCase() : "USER";
            if (!byType.containsKey(type)) byType.put(type, 0L);
            byType.put(type, byType.get(type) + 1);

            String continent = loc.getContinent() != null && !loc.getContinent().isBlank()
                    ? loc.getContinent() : "Unknown";
            String country = loc.getCountry() != null && !loc.getCountry().isBlank()
                    ? loc.getCountry() : "Unknown";

            byContinent.computeIfAbsent(continent, k -> new long[4]);
            long[] cArr = byContinent.get(continent);
            if ("USER".equals(type)) cArr[0]++;
            else if ("AGENT".equals(type)) cArr[1]++;
            else if ("BUSINESS".equals(type)) cArr[2]++;
            cArr[3]++;

            byCountry.computeIfAbsent(country, k -> new long[4]);
            long[] coArr = byCountry.get(country);
            if ("USER".equals(type)) coArr[0]++;
            else if ("AGENT".equals(type)) coArr[1]++;
            else if ("BUSINESS".equals(type)) coArr[2]++;
            coArr[3]++;
            countryToContinent.put(country, continent);
        }

        List<MapStatsResponse.ContinentStat> continents = byContinent.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[3], a.getValue()[3]))
                .map(e -> MapStatsResponse.ContinentStat.builder()
                        .name(e.getKey())
                        .users(e.getValue()[0])
                        .agents(e.getValue()[1])
                        .businesses(e.getValue()[2])
                        .total(e.getValue()[3])
                        .build())
                .toList();

        List<MapStatsResponse.CountryStat> countries = byCountry.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[3], a.getValue()[3]))
                .map(e -> MapStatsResponse.CountryStat.builder()
                        .name(e.getKey())
                        .continent(countryToContinent.getOrDefault(e.getKey(), "Unknown"))
                        .users(e.getValue()[0])
                        .agents(e.getValue()[1])
                        .businesses(e.getValue()[2])
                        .total(e.getValue()[3])
                        .build())
                .toList();

        return MapStatsResponse.builder()
                .continents(continents)
                .countries(countries)
                .byType(byType)
                .total(locations.size())
                .build();
    }

    // ==================== MEDIA STATS ====================

    public MediaStatsResponse getMediaStats() {
        long images = postMediaRepository.countByType(MediaType.IMAGE);
        long videos = postMediaRepository.countByType(MediaType.VIDEO);
        long total = images + videos;
        return MediaStatsResponse.builder()
                .totalImages(images)
                .totalVideos(videos)
                .totalMedia(total)
                .build();
    }

    // ==================== TRANSACTION REPORTS ====================

    public TransactionReportResponse getTransactionReports() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.with(LocalTime.MIN);
        LocalDateTime startOfWeek = now.minusDays(7);
        LocalDateTime startOfMonth = now.minusDays(30);

        BigDecimal dailyRev = paymentRepository.sumByStatusAndDateBetween(PaymentStatus.SUCCESS, startOfDay, now);
        BigDecimal weeklyRev = paymentRepository.sumByStatusAndDateBetween(PaymentStatus.SUCCESS, startOfWeek, now);
        BigDecimal monthlyRev = paymentRepository.sumByStatusAndDateBetween(PaymentStatus.SUCCESS, startOfMonth, now);

        long dailyCount = paymentRepository.countByStatusAndDateBetween(PaymentStatus.SUCCESS, startOfDay, now);
        long weeklyCount = paymentRepository.countByStatusAndDateBetween(PaymentStatus.SUCCESS, startOfWeek, now);
        long monthlyCount = paymentRepository.countByStatusAndDateBetween(PaymentStatus.SUCCESS, startOfMonth, now);

        return TransactionReportResponse.builder()
                .dailyRevenue(dailyRev != null ? dailyRev : BigDecimal.ZERO)
                .weeklyRevenue(weeklyRev != null ? weeklyRev : BigDecimal.ZERO)
                .monthlyRevenue(monthlyRev != null ? monthlyRev : BigDecimal.ZERO)
                .dailyTransactionCount(dailyCount)
                .weeklyTransactionCount(weeklyCount)
                .monthlyTransactionCount(monthlyCount)
                .build();
    }

    // ==================== AUDIENCE ANALYTICS ====================

    /**
     * Audience analytics for admin: by interests, location, demographics, behaviors.
     * Used for promotion targeting and audience insights.
     * @param fromDate optional - filter users by registration date (inclusive)
     * @param toDate optional - filter users by registration date (inclusive, end of day)
     */
    public AudienceAnalyticsResponse getAudienceAnalytics(java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        java.time.LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
        java.time.LocalDateTime to = toDate != null ? toDate.atTime(23, 59, 59, 999_999_999) : null;

        long totalUsers = userRepository.countBetween(from, to);

        // By interests (normalize: lowercase, trim, split comma)
        List<AudienceAnalyticsResponse.InterestStat> byInterests = buildInterestStats(from, to);

        // By location
        List<AudienceAnalyticsResponse.LocationStat> byCountry = buildLocationStats(
                userRepository.countGroupByCountryBetween(from, to));
        List<AudienceAnalyticsResponse.LocationStat> byRegion = buildLocationStats(
                userRepository.countGroupByRegionBetween(from, to));
        List<AudienceAnalyticsResponse.LocationStat> byCity = buildLocationStats(
                userRepository.countGroupByCityBetween(from, to));
        List<AudienceAnalyticsResponse.LocationStat> byContinent = buildContinentStats(byCountry);

        // By demographics
        List<AudienceAnalyticsResponse.DemographicStat> byAgeBand = buildAgeBandStats(from, to);
        List<AudienceAnalyticsResponse.DemographicStat> byGender = buildDemographicStats(
                userRepository.countGroupByGenderBetween(from, to));

        // By behaviors (derived; not date-filtered for simplicity)
        List<AudienceAnalyticsResponse.BehaviorStat> byBehaviors = buildBehaviorStats();

        return AudienceAnalyticsResponse.builder()
                .byInterests(byInterests)
                .byContinent(byContinent)
                .byCountry(byCountry)
                .byRegion(byRegion)
                .byCity(byCity)
                .byAgeBand(byAgeBand)
                .byGender(byGender)
                .byBehaviors(byBehaviors)
                .totalUsers(totalUsers)
                .build();
    }

    private List<AudienceAnalyticsResponse.InterestStat> buildInterestStats(java.time.LocalDateTime from, java.time.LocalDateTime to) {
        Map<String, Long> counts = new HashMap<>();
        List<String> raw = userRepository.findAllInterestsStringsBetween(from, to);
        for (String s : raw) {
            if (s == null || s.isBlank()) continue;
            for (String part : s.split(",")) {
                String key = part.trim().toLowerCase();
                if (key.isEmpty()) continue;
                counts.merge(key, 1L, Long::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> AudienceAnalyticsResponse.InterestStat.builder()
                        .interest(capitalizeFirst(e.getKey()))
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private static String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private List<AudienceAnalyticsResponse.LocationStat> buildLocationStats(List<Object[]> rows) {
        return rows.stream()
                .map(r -> AudienceAnalyticsResponse.LocationStat.builder()
                        .name(String.valueOf(r[0]))
                        .count(((Number) r[1]).longValue())
                        .build())
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
    }

    /** Aggregate byCountry into byContinent using ContinentHelper (country -> continent from map/lat-long). */
    private List<AudienceAnalyticsResponse.LocationStat> buildContinentStats(List<AudienceAnalyticsResponse.LocationStat> byCountry) {
        Map<String, Long> continentCounts = new HashMap<>();
        for (AudienceAnalyticsResponse.LocationStat c : byCountry) {
            String continent = ContinentHelper.getContinent(c.getName());
            continentCounts.merge(continent, c.getCount(), Long::sum);
        }
        return continentCounts.entrySet().stream()
                .map(e -> AudienceAnalyticsResponse.LocationStat.builder()
                        .name(e.getKey())
                        .count(e.getValue())
                        .build())
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
    }

    private List<AudienceAnalyticsResponse.DemographicStat> buildDemographicStats(List<Object[]> rows) {
        return rows.stream()
                .map(r -> AudienceAnalyticsResponse.DemographicStat.builder()
                        .bucket(String.valueOf(r[0]))
                        .count(((Number) r[1]).longValue())
                        .build())
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
    }

    private List<AudienceAnalyticsResponse.DemographicStat> buildAgeBandStats(java.time.LocalDateTime from, java.time.LocalDateTime to) {
        String[] labels = {"Under 18", "18-24", "25-34", "35-44", "45-54", "55-64", "65+"};
        long[] counts = new long[labels.length];
        LocalDate today = LocalDate.now();

        for (LocalDate dob : userRepository.findAllDateOfBirthBetween(from, to)) {
            int age = (int) java.time.temporal.ChronoUnit.YEARS.between(dob, today);
            if (age < 18) counts[0]++;
            else if (age < 25) counts[1]++;
            else if (age < 35) counts[2]++;
            else if (age < 45) counts[3]++;
            else if (age < 55) counts[4]++;
            else if (age < 65) counts[5]++;
            else counts[6]++;
        }

        List<AudienceAnalyticsResponse.DemographicStat> result = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            result.add(AudienceAnalyticsResponse.DemographicStat.builder()
                    .bucket(labels[i])
                    .count(counts[i])
                    .build());
        }
        return result;
    }

    private List<AudienceAnalyticsResponse.BehaviorStat> buildBehaviorStats() {
        long onlineShoppers = orderRepository.countDistinctBuyers();
        long engagedReactors = postReactionRepository.countDistinctReactors();
        long engagedCommenters = commentRepository.countDistinctCommenters();

        List<AudienceAnalyticsResponse.BehaviorStat> list = new ArrayList<>();
        list.add(AudienceAnalyticsResponse.BehaviorStat.builder()
                .behavior("Online shoppers")
                .count(onlineShoppers)
                .build());
        list.add(AudienceAnalyticsResponse.BehaviorStat.builder()
                .behavior("Engaged (reactions)")
                .count(engagedReactors)
                .build());
        list.add(AudienceAnalyticsResponse.BehaviorStat.builder()
                .behavior("Engaged (comments)")
                .count(engagedCommenters)
                .build());
        return list;
    }

    // ==================== ANALYTICS (DAU/MAU) ====================

    public AnalyticsResponse getAnalytics() {
        LocalDateTime dauSince = LocalDateTime.now().minusDays(1);
        LocalDateTime mauSince = LocalDateTime.now().minusDays(30);
        long dau = userRepository.countDistinctByLastSeenAfter(dauSince);
        long mau = userRepository.countDistinctByLastSeenAfter(mauSince);
        return AnalyticsResponse.builder()
                .dailyActiveUsers(dau)
                .monthlyActiveUsers(mau)
                .build();
    }

    // ==================== EXPORT ====================

    public List<User> getAllUsersForExport() {
        return userRepository.findAll();
    }

    public List<Business> getAllBusinessesForExport() {
        return businessRepository.findAll();
    }

    // ==================== IMPERSONATION ====================

    // ==================== ORDERS (ADMIN) ====================

    public PagedResponse<OrderResponse> getAllOrders(OrderStatus status, int page, int size) {
        return orderService.getAllOrdersForAdmin(status, page, size);
    }

    public OrderResponse getOrderById(UUID orderId) {
        return orderService.getOrderByIdForAdmin(orderId);
    }

    public OrderResponse updateOrderStatus(UUID orderId, UUID adminId, UpdateOrderStatusRequest request) {
        return orderService.updateOrderStatusForAdmin(orderId, adminId, request);
    }

    // ==================== PRODUCTS (ADMIN) ====================

    public PagedResponse<ProductResponse> getAllProducts(UUID businessId, Boolean active, String search, int page, int size) {
        return productService.getAllProductsForAdmin(businessId, active, search, page, size);
    }

    public ProductResponse setProductActive(UUID productId, boolean isActive) {
        return productService.setProductActiveForAdmin(productId, isActive);
    }

    public void deleteProduct(UUID productId) {
        productService.deleteProductForAdmin(productId);
    }

    // ==================== PROMOTIONS (ADMIN) ====================

    public PagedResponse<PromotionResponse> getAllPromotions(PromotionStatus status, PromotionType type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Promotion> promotions;
        if (status != null && type != null) {
            promotions = promotionRepository.findByTypeAndStatusOrderByCreatedAtDesc(type, status, pageable);
        } else if (status != null) {
            promotions = promotionRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else if (type != null) {
            promotions = promotionRepository.findByTypeOrderByCreatedAtDesc(type, pageable);
        } else {
            promotions = promotionRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return PagedResponse.<PromotionResponse>builder()
                .content(promotions.getContent().stream()
                        .map(promotionService::mapToAdminResponse)
                        .collect(Collectors.toList()))
                .page(promotions.getNumber())
                .size(promotions.getSize())
                .totalElements(promotions.getTotalElements())
                .totalPages(promotions.getTotalPages())
                .last(promotions.isLast())
                .first(promotions.isFirst())
                .build();
    }

    public Map<String, Object> getPromotionsStats() {
        long total = promotionRepository.count();
        long active = promotionRepository.countByStatus(PromotionStatus.ACTIVE);
        long pending = promotionRepository.countByStatus(PromotionStatus.PENDING);
        long pendingApproval = promotionRepository.countByStatus(PromotionStatus.PENDING_APPROVAL);
        long paused = promotionRepository.countByStatus(PromotionStatus.PAUSED);
        long completed = promotionRepository.countByStatus(PromotionStatus.COMPLETED);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("active", active);
        stats.put("pending", pending);
        stats.put("pendingApproval", pendingApproval);
        stats.put("paused", paused);
        stats.put("completed", completed);
        return stats;
    }

    @Transactional
    public PromotionResponse adminApprovePromotion(UUID promotionId, UUID adminId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", promotionId));
        if (promotion.getStatus() != PromotionStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only promotions pending approval can be approved");
        }
        promotion.setStatus(PromotionStatus.ACTIVE);
        promotionRepository.save(promotion);
        auditLogService.log(adminId, "PROMOTION_APPROVED", "Promotion", promotionId, "Admin approved promotion (policy check passed)", null, null, null);
        notificationService.sendNotification(promotion.getUser(), null, NotificationType.PROMOTION_APPROVED,
                promotionId, "Tangazo lako limeidhinishwa. Sasa linaonyeshwa kwa watu.");
        log.info("Admin {} approved promotion {}", adminId, promotionId);
        return promotionService.mapToAdminResponse(promotion);
    }

    @Transactional
    public PromotionResponse adminPausePromotion(UUID promotionId, UUID adminId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", promotionId));
        if (promotion.getStatus() != PromotionStatus.ACTIVE) {
            throw new BadRequestException("Only active promotions can be paused");
        }
        promotion.setStatus(PromotionStatus.PAUSED);
        promotionRepository.save(promotion);
        auditLogService.log(adminId, "PROMOTION_PAUSED", "Promotion", promotionId, "Admin paused promotion", null, null, null);
        notificationService.sendNotification(promotion.getUser(), null, NotificationType.PROMOTION_PAUSED,
                promotionId, "Tangazo lako limepauzwa na msimamizi.");
        log.info("Admin {} paused promotion {}", adminId, promotionId);
        return promotionService.mapToAdminResponse(promotion);
    }

    @Transactional
    public PromotionResponse adminResumePromotion(UUID promotionId, UUID adminId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", promotionId));
        if (promotion.getStatus() != PromotionStatus.PAUSED) {
            throw new BadRequestException("Only paused promotions can be resumed");
        }
        promotion.setStatus(PromotionStatus.ACTIVE);
        promotionRepository.save(promotion);
        auditLogService.log(adminId, "PROMOTION_RESUMED", "Promotion", promotionId, "Admin resumed promotion", null, null, null);
        log.info("Admin {} resumed promotion {}", adminId, promotionId);
        return promotionService.mapToAdminResponse(promotion);
    }

    @Transactional
    public PromotionResponse adminRejectPromotion(UUID promotionId, UUID adminId, String reason) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", "id", promotionId));
        if (promotion.getStatus() != PromotionStatus.PENDING && promotion.getStatus() != PromotionStatus.PENDING_APPROVAL
                && promotion.getStatus() != PromotionStatus.ACTIVE) {
            throw new BadRequestException("Cannot reject promotion in current state");
        }
        promotion.setStatus(PromotionStatus.REJECTED);
        promotionRepository.save(promotion);
        auditLogService.log(adminId, "PROMOTION_REJECTED", "Promotion", promotionId, reason != null ? reason : "Admin rejected", null, null, null);
        String msg = reason != null && !reason.isBlank()
                ? "Tangazo lako limekataliwa: " + reason
                : "Tangazo lako limekataliwa na msimamizi.";
        notificationService.sendNotification(promotion.getUser(), null, NotificationType.PROMOTION_REJECTED,
                promotionId, msg);
        log.info("Admin {} rejected promotion {}: {}", adminId, promotionId, reason);
        return promotionService.mapToAdminResponse(promotion);
    }

    // ==================== IMPERSONATION ====================

    /**
     * Generate login tokens for a user (admin impersonation).
     * Admin can use these tokens to access the app as that user.
     */
    public AuthResponse impersonateUser(UUID userId, UUID adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String username = user.getPhone() != null && !user.getPhone().isBlank()
                ? user.getPhone()
                : (user.getEmail() != null && !user.getEmail().isBlank()
                        ? user.getEmail()
                        : user.getId().toString());

        var userDetails = userDetailsService.loadUserByUsername(username);
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        auditLogService.log(adminId, "IMPERSONATE", "User", userId,
                "Admin impersonated user " + user.getName(), null, null, null);

        log.warn("Admin {} impersonating user {}", adminId, userId);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(mapUserToResponse(user))
                .build();
    }
}
