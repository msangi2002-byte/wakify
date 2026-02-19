package com.wakilfly.service;

import com.wakilfly.dto.request.CreateBusinessRequestRequest;
import com.wakilfly.dto.response.BusinessRequestResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.Agent;
import com.wakilfly.model.BusinessRequest;
import com.wakilfly.model.BusinessRequestStatus;
import com.wakilfly.model.User;
import com.wakilfly.repository.AgentRepository;
import com.wakilfly.repository.BusinessRequestRepository;
import com.wakilfly.service.SystemSettingsService;
import com.wakilfly.service.NotificationService;
import com.wakilfly.model.NotificationType;
import com.wakilfly.repository.BusinessRepository;
import com.wakilfly.repository.UserRepository;
import com.wakilfly.service.BusinessRegistrationPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessRequestService {

    private final BusinessRequestRepository businessRequestRepository;
    private final SystemSettingsService systemSettingsService;
    private final UserRepository userRepository;
    private final AgentRepository agentRepository;
    private final BusinessRepository businessRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final BusinessRegistrationPlanService businessRegistrationPlanService;

    /**
     * User requests to become a business. Request is sent to the selected agent (if any) with user location.
     * No payment at this step – agent will visit, then payment/approval flow happens later.
     */
    @Transactional
    public BusinessRequestResponse create(UUID userId, CreateBusinessRequestRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (businessRepository.findByOwnerId(user.getId()).isPresent()) {
            throw new BadRequestException("You already have a registered business.");
        }
        Agent agent = null;
        if (request.getAgentCode() != null && !request.getAgentCode().trim().isEmpty()) {
            agent = agentRepository.findByAgentCode(request.getAgentCode().trim())
                    .orElseThrow(() -> new BadRequestException("Invalid agent code: " + request.getAgentCode()));
            if (agent.getStatus() != com.wakilfly.model.AgentStatus.ACTIVE) {
                throw new BadRequestException("Selected agent is not active. Please choose another agent.");
            }
        }
        // Optional: validate plan if provided (for when payment is triggered later by agent)
        if (request.getBusinessPlanId() != null) {
            businessRegistrationPlanService.findById(request.getBusinessPlanId())
                    .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                    .orElseThrow(() -> new BadRequestException("Invalid or inactive business plan selected."));
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
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .status(BusinessRequestStatus.PENDING)
                .build();
        br = businessRequestRepository.save(br);

        if (agent != null) {
            notificationService.sendNotification(
                    agent.getUser(),
                    user,
                    NotificationType.BUSINESS_REQUEST_RECEIVED,
                    br.getId(),
                    user.getName() + " requested to become a business: " + br.getBusinessName());
        }
        log.info("Business request {} created by user {}; sent to agent (no payment yet)", br.getId(), userId);
        return mapToResponse(br);
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
                .userLatitude(br.getLatitude() != null ? br.getLatitude() : (br.getUser() != null ? br.getUser().getLatitude() : null))
                .userLongitude(br.getLongitude() != null ? br.getLongitude() : (br.getUser() != null ? br.getUser().getLongitude() : null))
                .agentLatitude(br.getAgent() != null ? br.getAgent().getLatitude() : null)
                .agentLongitude(br.getAgent() != null ? br.getAgent().getLongitude() : null)
                .nidaNumber(br.getNidaNumber())
                .tinNumber(br.getTinNumber())
                .companyName(br.getCompanyName())
                .idDocumentUrl(br.getIdDocumentUrl())
                .idBackDocumentUrl(br.getIdBackDocumentUrl())
                .build();
    }

    /**
     * Get one business request by id; only for the request's agent.
     */
    @Transactional(readOnly = true)
    public BusinessRequestResponse getByIdForAgent(UUID requestId, UUID agentUserId) {
        BusinessRequest br = businessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Business request", "id", requestId));
        if (br.getAgent() == null || !br.getAgent().getUser().getId().equals(agentUserId)) {
            throw new BadRequestException("You can only view your own business requests.");
        }
        return mapToResponse(br);
    }

    /**
     * Agent updates document/details for a business request (after visiting the user).
     */
    @Transactional
    public BusinessRequestResponse updateRequestDetailsByAgent(UUID requestId, UUID agentUserId,
            String nidaNumber, String tinNumber, String companyName, String idDocumentUrl, String idBackDocumentUrl) {
        BusinessRequest br = businessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Business request", "id", requestId));
        if (br.getAgent() == null || !br.getAgent().getUser().getId().equals(agentUserId)) {
            throw new BadRequestException("You can only update your own business requests.");
        }
        if (nidaNumber != null) br.setNidaNumber(nidaNumber.trim().isEmpty() ? null : nidaNumber.trim());
        if (tinNumber != null) br.setTinNumber(tinNumber.trim().isEmpty() ? null : tinNumber.trim());
        if (companyName != null) br.setCompanyName(companyName.trim().isEmpty() ? null : companyName.trim());
        if (idDocumentUrl != null) br.setIdDocumentUrl(idDocumentUrl.trim().isEmpty() ? null : idDocumentUrl.trim());
        if (idBackDocumentUrl != null) br.setIdBackDocumentUrl(idBackDocumentUrl.trim().isEmpty() ? null : idBackDocumentUrl.trim());
        br = businessRequestRepository.save(br);
        return mapToResponse(br);
    }

    /**
     * Agent approves the request after visit: create business, set user role to BUSINESS, set request to CONVERTED, create commission.
     * Allowed when status is PENDING (request-only, no payment yet) or PAID (user already paid).
     */
    @Transactional
    public BusinessRequestResponse approveByAgent(UUID requestId, UUID agentUserId) {
        BusinessRequest br = businessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Business request", "id", requestId));
        if (br.getAgent() == null || !br.getAgent().getUser().getId().equals(agentUserId)) {
            throw new BadRequestException("You can only approve your own business requests.");
        }
        if (br.getStatus() != BusinessRequestStatus.PENDING && br.getStatus() != BusinessRequestStatus.PAID) {
            throw new BadRequestException("Request can only be approved when PENDING or PAID. Current status: " + br.getStatus());
        }
        if (businessRepository.findByOwnerId(br.getUser().getId()).isPresent()) {
            br.setStatus(BusinessRequestStatus.CONVERTED);
            businessRequestRepository.save(br);
            return mapToResponse(br);
        }
        paymentService.completeBusinessFromRequest(br);
        br = businessRequestRepository.findById(requestId).orElse(br);
        log.info("Business request {} approved by agent (user {}); business created", requestId, br.getUser().getId());
        return mapToResponse(br);
    }
}
