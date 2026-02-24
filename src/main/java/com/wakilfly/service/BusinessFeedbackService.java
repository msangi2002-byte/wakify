package com.wakilfly.service;

import com.wakilfly.dto.request.BusinessFeedbackRequest;
import com.wakilfly.dto.response.BusinessFeedbackResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.PostResponse;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.Business;
import com.wakilfly.model.BusinessFeedback;
import com.wakilfly.model.User;
import com.wakilfly.repository.BusinessFeedbackRepository;
import com.wakilfly.repository.BusinessRepository;
import com.wakilfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessFeedbackService {

    private final BusinessFeedbackRepository feedbackRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;

    @Transactional
    public BusinessFeedbackResponse addFeedback(UUID userId, UUID businessId, BusinessFeedbackRequest request) {
        User user = userRepository.getReferenceById(userId);
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));

        BusinessFeedback feedback = BusinessFeedback.builder()
                .business(business)
                .user(user)
                .content(request.getContent().trim())
                .build();
        feedback = feedbackRepository.save(feedback);

        return mapToResponse(feedback);
    }

    public PagedResponse<BusinessFeedbackResponse> getMyBusinessFeedback(UUID ownerId, int page, int size) {
        Business business = businessRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "owner", ownerId));

        Pageable pageable = PageRequest.of(page, size);
        Page<BusinessFeedback> pageResult = feedbackRepository.findByBusinessIdOrderByCreatedAtDesc(business.getId(), pageable);

        return PagedResponse.<BusinessFeedbackResponse>builder()
                .content(pageResult.getContent().stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()))
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .first(pageResult.isFirst())
                .build();
    }

    @Transactional
    public BusinessFeedbackResponse markAsRead(UUID feedbackId, UUID ownerId) {
        Business business = businessRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "owner", ownerId));
        BusinessFeedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback", "id", feedbackId));
        if (!feedback.getBusiness().getId().equals(business.getId())) {
            throw new BadRequestException("Feedback does not belong to your business");
        }
        feedback.setRead(true);
        feedback.setReadAt(java.time.LocalDateTime.now());
        feedback = feedbackRepository.save(feedback);
        return mapToResponse(feedback);
    }

    @Transactional
    public int markAllAsRead(UUID ownerId) {
        Business business = businessRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "owner", ownerId));
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<BusinessFeedback> unread = feedbackRepository.findByBusinessIdAndReadFalse(business.getId());
        for (BusinessFeedback f : unread) {
            f.setRead(true);
            f.setReadAt(now);
            feedbackRepository.save(f);
        }
        return unread.size();
    }

    private BusinessFeedbackResponse mapToResponse(BusinessFeedback f) {
        User u = f.getUser();
        return BusinessFeedbackResponse.builder()
                .id(f.getId())
                .content(f.getContent())
                .createdAt(f.getCreatedAt())
                .read(f.getRead())
                .readAt(f.getReadAt())
                .user(PostResponse.UserSummary.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .profilePic(u.getProfilePic())
                        .build())
                .build();
    }
}
