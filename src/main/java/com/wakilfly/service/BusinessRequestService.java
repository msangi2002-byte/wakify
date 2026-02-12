package com.wakilfly.service;

import com.wakilfly.dto.request.CreateBusinessRequestRequest;
import com.wakilfly.dto.response.BusinessRequestResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.Agent;
import com.wakilfly.model.BusinessRequest;
import com.wakilfly.model.BusinessRequestStatus;
import com.wakilfly.model.PaymentType;
import com.wakilfly.model.User;
import com.wakilfly.repository.AgentRepository;
import com.wakilfly.repository.BusinessRequestRepository;
import com.wakilfly.repository.BusinessRepository;
import com.wakilfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessRequestService {

    private static final BigDecimal BUSINESS_ACTIVATION_FEE = new BigDecimal("10000.00");

    private final BusinessRequestRepository businessRequestRepository;
    private final UserRepository userRepository;
    private final AgentRepository agentRepository;
    private final BusinessRepository businessRepository;
    private final PaymentService paymentService;

    /**
     * User with account requests to become a business. System pushes USSD payment;
     * after payment completes, system approves and creates the business (no agent approval).
     */
    @Transactional
    public BusinessRequestResponse create(UUID userId, CreateBusinessRequestRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (businessRepository.findByOwnerId(user.getId()).isPresent()) {
            throw new BadRequestException("You already have a registered business.");
        }
        Agent agent = agentRepository.findByAgentCode(request.getAgentCode().trim())
                .orElseThrow(() -> new BadRequestException("Invalid agent code: " + request.getAgentCode()));
        if (agent.getStatus() != com.wakilfly.model.AgentStatus.ACTIVE) {
            throw new BadRequestException("Selected agent is not active. Please choose another agent.");
        }
        BusinessRequest br = BusinessRequest.builder()
                .user(user)
                .agent(agent)
                .businessName(request.getBusinessName().trim())
                .ownerPhone(request.getOwnerPhone().trim())
                .category(request.getCategory() != null ? request.getCategory().trim() : null)
                .region(request.getRegion() != null ? request.getRegion().trim() : null)
                .district(request.getDistrict() != null ? request.getDistrict().trim() : null)
                .ward(request.getWard() != null ? request.getWard().trim() : null)
                .street(request.getStreet() != null ? request.getStreet().trim() : null)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .status(BusinessRequestStatus.PENDING)
                .build();
        br = businessRequestRepository.save(br);

        // System completes the request: push USSD payment; after payment, system will create business and approve user
        String phone = request.getOwnerPhone().trim();
        String description = "Business activation: " + br.getBusinessName();
        String orderId = paymentService.initiatePayment(
                userId,
                BUSINESS_ACTIVATION_FEE,
                PaymentType.BUSINESS_ACTIVATION,
                phone,
                description,
                br.getId(),
                "BUSINESS_REQUEST");
        log.info("Business request {} created by user {}; USSD payment initiated orderId={}", br.getId(), userId, orderId);
        BusinessRequestResponse response = mapToResponse(br);
        response.setPaymentOrderId(orderId);
        return response;
    }

    @Transactional(readOnly = true)
    public PagedResponse<BusinessRequestResponse> findByAgentId(UUID agentUserId, int page, int size) {
        Agent agent = agentRepository.findByUserId(agentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));
        Page<BusinessRequest> pageResult = businessRequestRepository.findByAgentIdOrderByCreatedAtDesc(
                agent.getId(), PageRequest.of(page, size));
        return PagedResponse.<BusinessRequestResponse>builder()
                .content(pageResult.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()))
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .first(pageResult.isFirst())
                .build();
    }

    /**
     * Get current user's business requests (for "I want to be a business" status in UI).
     */
    @Transactional(readOnly = true)
    public PagedResponse<BusinessRequestResponse> findMyRequests(UUID userId, int page, int size) {
        Page<BusinessRequest> pageResult = businessRequestRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size));
        return PagedResponse.<BusinessRequestResponse>builder()
                .content(pageResult.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()))
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .first(pageResult.isFirst())
                .build();
    }

    private BusinessRequestResponse mapToResponse(BusinessRequest br) {
        return BusinessRequestResponse.builder()
                .id(br.getId())
                .userId(br.getUser() != null ? br.getUser().getId() : null)
                .userName(br.getUser() != null ? br.getUser().getName() : null)
                .userPhone(br.getUser() != null ? br.getUser().getPhone() : null)
                .agentId(br.getAgent() != null ? br.getAgent().getId() : null)
                .agentCode(br.getAgent() != null ? br.getAgent().getAgentCode() : null)
                .businessName(br.getBusinessName())
                .ownerPhone(br.getOwnerPhone())
                .category(br.getCategory())
                .region(br.getRegion())
                .district(br.getDistrict())
                .ward(br.getWard())
                .street(br.getStreet())
                .description(br.getDescription())
                .status(br.getStatus())
                .createdAt(br.getCreatedAt())
                .updatedAt(br.getUpdatedAt())
                .build();
    }
}
