package com.wakilfly.controller;

import com.wakilfly.dto.response.*;
import com.wakilfly.model.*;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.AdminService;
import com.wakilfly.service.AuditLogService;
import com.wakilfly.service.GiftService;
import com.wakilfly.service.ReportService;
import com.wakilfly.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final GiftService giftService;
    private final ReportService reportService;
    private final AuditLogService auditLogService;
    private final CustomUserDetailsService userDetailsService;
    private final SystemConfigService systemConfigService;

    // ==================== DASHBOARD ====================

    /**
     * Get admin dashboard statistics
     * GET /api/v1/admin/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        AdminDashboardResponse dashboard = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    // ==================== USER MANAGEMENT ====================

    /**
     * Get all users
     * GET /api/v1/admin/users
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive) {
        PagedResponse<UserResponse> users = adminService.getAllUsers(page, size, role, isActive);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * Update user status (activate/deactivate)
     * PUT /api/v1/admin/users/{id}/status
     */
    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        boolean isActive = (Boolean) request.get("isActive");
        String reason = (String) request.getOrDefault("reason", "");

        UserResponse user = adminService.updateUserStatus(id, adminId, isActive, reason);
        return ResponseEntity.ok(ApiResponse.success("User status updated", user));
    }

    /**
     * Update user role
     * PUT /api/v1/admin/users/{id}/role
     */
    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        Role role = Role.valueOf((String) request.get("role"));
        String reason = (String) request.getOrDefault("reason", "");

        UserResponse user = adminService.updateUserRole(id, adminId, role, reason);
        return ResponseEntity.ok(ApiResponse.success("User role updated", user));
    }

    // ==================== BUSINESS MANAGEMENT ====================

    /**
     * Get all businesses
     * GET /api/v1/admin/businesses
     */
    @GetMapping("/businesses")
    public ResponseEntity<ApiResponse<PagedResponse<BusinessResponse>>> getBusinesses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) BusinessStatus status) {
        PagedResponse<BusinessResponse> businesses = adminService.getAllBusinesses(page, size, status);
        return ResponseEntity.ok(ApiResponse.success(businesses));
    }

    /**
     * Update business status
     * PUT /api/v1/admin/businesses/{id}/status
     */
    @PutMapping("/businesses/{id}/status")
    public ResponseEntity<ApiResponse<BusinessResponse>> updateBusinessStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        BusinessStatus status = BusinessStatus.valueOf((String) request.get("status"));
        String reason = (String) request.getOrDefault("reason", "");

        BusinessResponse business = adminService.updateBusinessStatus(id, adminId, status, reason);
        return ResponseEntity.ok(ApiResponse.success("Business status updated", business));
    }

    /**
     * Verify business
     * POST /api/v1/admin/businesses/{id}/verify
     */
    @PostMapping("/businesses/{id}/verify")
    public ResponseEntity<ApiResponse<BusinessResponse>> verifyBusiness(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        BusinessResponse business = adminService.verifyBusiness(id, adminId);
        return ResponseEntity.ok(ApiResponse.success("Business verified", business));
    }

    /**
     * Verify user (Blue Tick)
     * POST /api/v1/admin/users/{id}/verify
     */
    @PostMapping("/users/{id}/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        Map<String, Object> result = adminService.verifyUser(id, adminId);
        return ResponseEntity.ok(ApiResponse.success("User verified (Blue Tick)", result));
    }

    // ==================== AGENT MANAGEMENT ====================

    /**
     * Get all agents
     * GET /api/v1/admin/agents
     */
    @GetMapping("/agents")
    public ResponseEntity<ApiResponse<PagedResponse<AgentResponse>>> getAgents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) AgentStatus status) {
        PagedResponse<AgentResponse> agents = adminService.getAllAgents(page, size, status);
        return ResponseEntity.ok(ApiResponse.success(agents));
    }

    /**
     * Update agent status
     * PUT /api/v1/admin/agents/{id}/status
     */
    @PutMapping("/agents/{id}/status")
    public ResponseEntity<ApiResponse<AgentResponse>> updateAgentStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        AgentStatus status = AgentStatus.valueOf((String) request.get("status"));
        String reason = (String) request.getOrDefault("reason", "");

        AgentResponse agent = adminService.updateAgentStatus(id, adminId, status, reason);
        return ResponseEntity.ok(ApiResponse.success("Agent status updated", agent));
    }

    /**
     * Verify agent
     * POST /api/v1/admin/agents/{id}/verify
     */
    @PostMapping("/agents/{id}/verify")
    public ResponseEntity<ApiResponse<AgentResponse>> verifyAgent(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        AgentResponse agent = adminService.verifyAgent(id, adminId);
        return ResponseEntity.ok(ApiResponse.success("Agent verified", agent));
    }

    // ==================== WITHDRAWAL MANAGEMENT ====================

    /**
     * Get pending withdrawals
     * GET /api/v1/admin/withdrawals
     */
    @GetMapping("/withdrawals")
    public ResponseEntity<ApiResponse<PagedResponse<WithdrawalResponse>>> getWithdrawals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<WithdrawalResponse> withdrawals = adminService.getPendingWithdrawals(page, size);
        return ResponseEntity.ok(ApiResponse.success(withdrawals));
    }

    /**
     * Process a withdrawal (approve/reject)
     * POST /api/v1/admin/withdrawals/{id}/process
     */
    @PostMapping("/withdrawals/{id}/process")
    public ResponseEntity<ApiResponse<WithdrawalResponse>> processWithdrawal(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        boolean approve = (Boolean) request.get("approve");
        String notes = (String) request.getOrDefault("notes", "");
        String transactionId = (String) request.get("transactionId");

        WithdrawalResponse withdrawal = adminService.processWithdrawal(id, adminId, approve, notes, transactionId);
        return ResponseEntity.ok(ApiResponse.success(
                approve ? "Withdrawal approved" : "Withdrawal rejected", withdrawal));
    }

    /**
     * Get pending user (host) cash withdrawals (gift cash → pesa)
     * GET /api/v1/admin/user-withdrawals
     */
    @GetMapping("/user-withdrawals")
    public ResponseEntity<ApiResponse<PagedResponse<com.wakilfly.dto.response.UserCashWithdrawalResponse>>> getUserWithdrawals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<com.wakilfly.dto.response.UserCashWithdrawalResponse> list = giftService.getPendingUserWithdrawals(page, size);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * Process user cash withdrawal (approve = payout done; reject = refund to wallet)
     * POST /api/v1/admin/user-withdrawals/{id}/process
     */
    @PostMapping("/user-withdrawals/{id}/process")
    public ResponseEntity<ApiResponse<com.wakilfly.dto.response.UserCashWithdrawalResponse>> processUserWithdrawal(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        boolean approve = (Boolean) request.get("approve");
        String transactionId = (String) request.get("transactionId");
        String rejectionReason = (String) request.get("rejectionReason");
        com.wakilfly.dto.response.UserCashWithdrawalResponse w = giftService.processUserCashWithdrawal(id, approve, transactionId, rejectionReason);
        return ResponseEntity.ok(ApiResponse.success(approve ? "Withdrawal approved" : "Withdrawal rejected", w));
    }

    // ==================== REPORTS & MODERATION ====================

    /**
     * Get pending reports
     * GET /api/v1/admin/reports
     */
    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<PagedResponse<ReportResponse>>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "PENDING") ReportStatus status) {
        PagedResponse<ReportResponse> reports = reportService.getReportsByStatus(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    /**
     * Get reports by type
     * GET /api/v1/admin/reports/type/{type}
     */
    @GetMapping("/reports/type/{type}")
    public ResponseEntity<ApiResponse<PagedResponse<ReportResponse>>> getReportsByType(
            @PathVariable ReportType type,
            @RequestParam(defaultValue = "PENDING") ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ReportResponse> reports = reportService.getReportsByType(type, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    /**
     * Resolve a report
     * POST /api/v1/admin/reports/{id}/resolve
     */
    @PostMapping("/reports/{id}/resolve")
    public ResponseEntity<ApiResponse<ReportResponse>> resolveReport(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        String resolutionNotes = request.getOrDefault("notes", "");
        String actionTaken = request.getOrDefault("action", "");

        ReportResponse report = reportService.resolveReport(id, adminId, resolutionNotes, actionTaken);
        return ResponseEntity.ok(ApiResponse.success("Report resolved", report));
    }

    /**
     * Dismiss a report
     * POST /api/v1/admin/reports/{id}/dismiss
     */
    @PostMapping("/reports/{id}/dismiss")
    public ResponseEntity<ApiResponse<ReportResponse>> dismissReport(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        String reason = request.getOrDefault("reason", "No violation found");

        ReportResponse report = reportService.dismissReport(id, adminId, reason);
        return ResponseEntity.ok(ApiResponse.success("Report dismissed", report));
    }

    // ==================== AUDIT LOGS ====================

    /**
     * Get audit logs
     * GET /api/v1/admin/audit-logs
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PagedResponse<AuditLogResponse> logs = auditLogService.getAllLogs(page, size);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * Get audit logs by user
     * GET /api/v1/admin/audit-logs/user/{userId}
     */
    @GetMapping("/audit-logs/user/{userId}")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getAuditLogsByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PagedResponse<AuditLogResponse> logs = auditLogService.getLogsByUser(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * Get audit logs by entity
     * GET /api/v1/admin/audit-logs/entity/{entityId}
     */
    @GetMapping("/audit-logs/entity/{entityId}")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getAuditLogsByEntity(
            @PathVariable UUID entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PagedResponse<AuditLogResponse> logs = auditLogService.getLogsByEntity(entityId, page, size);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * Get audit logs by date range
     * GET /api/v1/admin/audit-logs/range
     */
    @GetMapping("/audit-logs/range")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getAuditLogsByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PagedResponse<AuditLogResponse> logs = auditLogService.getLogsByDateRange(startDate, endDate, page, size);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    // ==================== SETTINGS (FEE AMOUNTS) ====================

    /**
     * Get admin settings (agent register amount, business activation amount)
     * GET /api/v1/admin/settings
     */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getSettings() {
        Map<String, BigDecimal> settings = systemConfigService.getFeeAmounts();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    /**
     * Update fee amounts (agent register, business activation)
     * PUT /api/v1/admin/settings
     * Body: { "agentRegisterAmount": 20000, "businessActivationAmount": 10000 }
     */
    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> updateSettings(
            @RequestBody Map<String, Object> request) {
        BigDecimal agentRegisterAmount = request.get("agentRegisterAmount") != null
                ? new BigDecimal(request.get("agentRegisterAmount").toString()) : null;
        BigDecimal businessActivationAmount = request.get("businessActivationAmount") != null
                ? new BigDecimal(request.get("businessActivationAmount").toString()) : null;
        systemConfigService.updateFeeAmounts(agentRegisterAmount, businessActivationAmount);
        Map<String, BigDecimal> updated = systemConfigService.getFeeAmounts();
        return ResponseEntity.ok(ApiResponse.success("Settings updated", updated));
    }
}
