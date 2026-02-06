package com.wakilfly.service;

import com.wakilfly.dto.request.CreateReportRequest;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.ReportResponse;
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

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ProductRepository productRepository;
    private final BusinessRepository businessRepository;
    private final AuditLogService auditLogService;

    /**
     * Create a report
     */
    @Transactional
    public ReportResponse createReport(UUID reporterId, CreateReportRequest request) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", reporterId));

        // Check if already reported
        if (reportRepository.existsByReporterIdAndTargetIdAndType(reporterId, request.getTargetId(),
                request.getType())) {
            throw new BadRequestException("You have already reported this item");
        }

        // Validate target exists
        validateTarget(request.getType(), request.getTargetId());

        Report report = Report.builder()
                .reporter(reporter)
                .type(request.getType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .build();

        report = reportRepository.save(report);

        log.info("Report {} created by user {} for {} {}",
                report.getId(), reporterId, request.getType(), request.getTargetId());

        return mapToResponse(report);
    }

    /**
     * Get pending reports (admin)
     */
    public PagedResponse<ReportResponse> getPendingReports(int page, int size) {
        return getReportsByStatus(ReportStatus.PENDING, page, size);
    }

    /**
     * Get reports by status (admin)
     */
    public PagedResponse<ReportResponse> getReportsByStatus(ReportStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Report> reports = reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable);

        return PagedResponse.<ReportResponse>builder()
                .content(reports.getContent().stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()))
                .page(reports.getNumber())
                .size(reports.getSize())
                .totalElements(reports.getTotalElements())
                .totalPages(reports.getTotalPages())
                .last(reports.isLast())
                .first(reports.isFirst())
                .build();
    }

    /**
     * Get reports by type (admin)
     */
    public PagedResponse<ReportResponse> getReportsByType(ReportType type, ReportStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Report> reports = reportRepository.findByTypeAndStatusOrderByCreatedAtDesc(type, status, pageable);

        return PagedResponse.<ReportResponse>builder()
                .content(reports.getContent().stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()))
                .page(reports.getNumber())
                .size(reports.getSize())
                .totalElements(reports.getTotalElements())
                .totalPages(reports.getTotalPages())
                .last(reports.isLast())
                .first(reports.isFirst())
                .build();
    }

    /**
     * Resolve a report (admin)
     */
    @Transactional
    public ReportResponse resolveReport(UUID reportId, UUID adminId, String resolutionNotes, String actionTaken) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));

        report.setStatus(ReportStatus.RESOLVED);
        report.setResolvedBy(admin);
        report.setResolutionNotes(resolutionNotes);
        report.setActionTaken(actionTaken);
        report.setResolvedAt(LocalDateTime.now());

        report = reportRepository.save(report);

        // Log the action
        auditLogService.log(adminId, "REPORT_RESOLVED", "Report", reportId,
                "Report resolved: " + actionTaken, null, null, null);

        log.info("Report {} resolved by admin {} with action: {}", reportId, adminId, actionTaken);

        return mapToResponse(report);
    }

    /**
     * Dismiss a report (admin)
     */
    @Transactional
    public ReportResponse dismissReport(UUID reportId, UUID adminId, String reason) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));

        report.setStatus(ReportStatus.DISMISSED);
        report.setResolvedBy(admin);
        report.setResolutionNotes(reason);
        report.setResolvedAt(LocalDateTime.now());

        report = reportRepository.save(report);

        auditLogService.log(adminId, "REPORT_DISMISSED", "Report", reportId,
                "Report dismissed: " + reason, null, null, null);

        log.info("Report {} dismissed by admin {}", reportId, adminId);

        return mapToResponse(report);
    }

    /**
     * Get pending count
     */
    public long getPendingCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    private void validateTarget(ReportType type, UUID targetId) {
        switch (type) {
            case POST -> {
                if (!postRepository.existsById(targetId)) {
                    throw new ResourceNotFoundException("Post", "id", targetId);
                }
            }
            case PRODUCT -> {
                if (!productRepository.existsById(targetId)) {
                    throw new ResourceNotFoundException("Product", "id", targetId);
                }
            }
            case BUSINESS -> {
                if (!businessRepository.existsById(targetId)) {
                    throw new ResourceNotFoundException("Business", "id", targetId);
                }
            }
            case USER -> {
                if (!userRepository.existsById(targetId)) {
                    throw new ResourceNotFoundException("User", "id", targetId);
                }
            }
            case COMMENT, MESSAGE -> {
                // TODO: Add validation when repositories are available
            }
        }
    }

    private ReportResponse mapToResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .type(report.getType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .reporter(ReportResponse.UserSummary.builder()
                        .id(report.getReporter().getId())
                        .name(report.getReporter().getName())
                        .profilePic(report.getReporter().getProfilePic())
                        .build())
                .resolvedBy(report.getResolvedBy() != null ? ReportResponse.UserSummary.builder()
                        .id(report.getResolvedBy().getId())
                        .name(report.getResolvedBy().getName())
                        .profilePic(report.getResolvedBy().getProfilePic())
                        .build() : null)
                .resolutionNotes(report.getResolutionNotes())
                .actionTaken(report.getActionTaken())
                .resolvedAt(report.getResolvedAt())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
