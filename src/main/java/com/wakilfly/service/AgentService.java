package com.wakilfly.service;

import com.wakilfly.dto.request.AgentRegistrationRequest;
import com.wakilfly.dto.request.BusinessActivationRequest;
import com.wakilfly.dto.request.WithdrawalRequest;
import com.wakilfly.dto.response.*;
import com.wakilfly.model.*;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

        // Constants
        private static final BigDecimal AGENT_REGISTRATION_FEE = new BigDecimal("20000.00");
        private static final BigDecimal BUSINESS_ACTIVATION_FEE = new BigDecimal("10000.00");
        private static final BigDecimal AGENT_COMMISSION = new BigDecimal("5000.00");

        /**
         * Register a user as an Agent
         * User pays 20,000/= to become an agent
         */
        @Transactional
        public AgentResponse registerAsAgent(UUID userId, AgentRegistrationRequest request) {
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
                                .status(AgentStatus.PENDING) // Pending until payment is confirmed
                                .isVerified(false)
                                .totalEarnings(BigDecimal.ZERO)
                                .availableBalance(BigDecimal.ZERO)
                                .build();

                agent = agentRepository.save(agent);

                // Create payment record for registration fee
                Payment payment = Payment.builder()
                                .user(user)
                                .amount(AGENT_REGISTRATION_FEE)
                                .type(PaymentType.AGENT_REGISTRATION)
                                .status(PaymentStatus.PENDING)
                                .description("Agent registration fee")
                                .transactionId(generateTransactionId())
                                .paymentPhone(request.getPaymentPhone())
                                .build();

                paymentRepository.save(payment);

                // Update user role to AGENT
                user.setRole(Role.AGENT);
                userRepository.save(user);

                // TODO: Initiate payment via M-Pesa/Tigo Pesa
                log.info("Agent registration initiated for user {} with code {}", userId, agentCode);

                return mapToAgentResponse(agent);
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

                // Get or create business owner
                User owner;
                if (request.getOwnerId() != null) {
                        owner = userRepository.findById(request.getOwnerId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Owner", "id",
                                                        request.getOwnerId()));
                } else {
                        // Create new user for business owner
                        if (request.getOwnerPhone() == null || request.getOwnerName() == null) {
                                throw new BadRequestException(
                                                "Owner phone and name are required for new business owner");
                        }

                        // Check if phone already exists
                        if (userRepository.existsByPhone(request.getOwnerPhone())) {
                                throw new BadRequestException(
                                                "Phone number already registered. Use existing user ID instead.");
                        }

                        owner = User.builder()
                                        .name(request.getOwnerName())
                                        .phone(request.getOwnerPhone())
                                        .email(request.getOwnerEmail())
                                        .password(passwordEncoder.encode("temp" + System.currentTimeMillis())) // Temp
                                                                                                               // password
                                        .role(Role.BUSINESS)
                                        .isVerified(false)
                                        .isActive(true)
                                        .build();
                        owner = userRepository.save(owner);
                }

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
                                .status(BusinessStatus.PENDING) // Pending until payment confirmed
                                .isVerified(false)
                                .build();

                business = businessRepository.save(business);

                // Create payment record for activation fee
                Payment payment = Payment.builder()
                                .user(owner)
                                .amount(BUSINESS_ACTIVATION_FEE)
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
}
