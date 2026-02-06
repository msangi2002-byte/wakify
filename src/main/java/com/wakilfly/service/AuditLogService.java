package com.wakilfly.service;

import com.wakilfly.dto.response.AuditLogResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.model.AuditLog;
import com.wakilfly.model.User;
import com.wakilfly.repository.AuditLogRepository;
import com.wakilfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    /**
     * Log an action (async to not block main thread)
     */
    @Async
    @Transactional
    public void log(UUID userId, String action, String entityType, UUID entityId,
            String details, String oldValues, String newValues, String ipAddress) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("Cannot create audit log: user {} not found", userId);
                return;
            }

            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .oldValues(oldValues)
                    .newValues(newValues)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} - {} - {}", action, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }

    /**
     * Get all logs (paginated)
     */
    public PagedResponse<AuditLogResponse> getAllLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logs = auditLogRepository.findAllOrderByCreatedAtDesc(pageable);

        return mapToPagedResponse(logs);
    }

    /**
     * Get logs by user
     */
    public PagedResponse<AuditLogResponse> getLogsByUser(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logs = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return mapToPagedResponse(logs);
    }

    /**
     * Get logs by entity
     */
    public PagedResponse<AuditLogResponse> getLogsByEntity(UUID entityId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logs = auditLogRepository.findByEntityIdOrderByCreatedAtDesc(entityId, pageable);

        return mapToPagedResponse(logs);
    }

    /**
     * Get logs by entity type
     */
    public PagedResponse<AuditLogResponse> getLogsByEntityType(String entityType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logs = auditLogRepository.findByEntityTypeOrderByCreatedAtDesc(entityType, pageable);

        return mapToPagedResponse(logs);
    }

    /**
     * Get logs by date range
     */
    public PagedResponse<AuditLogResponse> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logs = auditLogRepository.findByDateRange(startDate, endDate, pageable);

        return mapToPagedResponse(logs);
    }

    /**
     * Search logs by action
     */
    public PagedResponse<AuditLogResponse> searchByAction(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logs = auditLogRepository.findByActionContainingIgnoreCaseOrderByCreatedAtDesc(query, pageable);

        return mapToPagedResponse(logs);
    }

    private PagedResponse<AuditLogResponse> mapToPagedResponse(Page<AuditLog> logs) {
        return PagedResponse.<AuditLogResponse>builder()
                .content(logs.getContent().stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()))
                .page(logs.getNumber())
                .size(logs.getSize())
                .totalElements(logs.getTotalElements())
                .totalPages(logs.getTotalPages())
                .last(logs.isLast())
                .first(logs.isFirst())
                .build();
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .oldValues(log.getOldValues())
                .newValues(log.getNewValues())
                .ipAddress(log.getIpAddress())
                .user(AuditLogResponse.UserSummary.builder()
                        .id(log.getUser().getId())
                        .name(log.getUser().getName())
                        .role(log.getUser().getRole().name())
                        .build())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
