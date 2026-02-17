package com.wakilfly.controller;

import com.wakilfly.dto.request.AdminSettingsUpdateRequest;
import com.wakilfly.dto.request.CreateAgentPackageRequest;
import com.wakilfly.dto.response.*;
import com.wakilfly.model.*;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.AdminService;
import com.wakilfly.service.AuditLogService;
import com.wakilfly.service.GiftService;
import com.wakilfly.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
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

    /**
     * Get chart data for revenue and users trends
     * GET /api/v1/admin/dashboard/charts?days=30
     */
    @GetMapping("/dashboard/charts")
    public ResponseEntity<ApiResponse<com.wakilfly.dto.response.ChartDataResponse>> getChartData(
            @RequestParam(defaultValue = "30") int days) {
        com.wakilfly.dto.response.ChartDataResponse chartData = adminService.getChartData(Math.min(days, 90));
        return ResponseEntity.ok(ApiResponse.success(chartData));
    }

    /**
     * Get map locations (businesses with coordinates)
     * GET /api/v1/admin/map/locations
     */
    @GetMapping("/map/locations")
    public ResponseEntity<ApiResponse<java.util.List<com.wakilfly.dto.response.MapLocationResponse>>> getMapLocations() {
        java.util.List<com.wakilfly.dto.response.MapLocationResponse> locations = adminService.getMapLocations();
        return ResponseEntity.ok(ApiResponse.success(locations));
    }

    /**
     * Get media stats (images, videos count)
     * GET /api/v1/admin/media-stats
     */
    @GetMapping("/media-stats")
    public ResponseEntity<ApiResponse<com.wakilfly.dto.response.MediaStatsResponse>> getMediaStats() {
        com.wakilfly.dto.response.MediaStatsResponse stats = adminService.getMediaStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Get transaction reports (daily/weekly/monthly)
     * GET /api/v1/admin/transaction-reports
     */
    @GetMapping("/transaction-reports")
    public ResponseEntity<ApiResponse<com.wakilfly.dto.response.TransactionReportResponse>> getTransactionReports() {
        com.wakilfly.dto.response.TransactionReportResponse reports = adminService.getTransactionReports();
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    /**
     * Get analytics (DAU, MAU)
     * GET /api/v1/admin/analytics
     */
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<com.wakilfly.dto.response.AnalyticsResponse>> getAnalytics() {
        com.wakilfly.dto.response.AnalyticsResponse analytics = adminService.getAnalytics();
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    /**
     * Export users as CSV
     * GET /api/v1/admin/users/export
     */
    @GetMapping(value = "/users/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportUsers() {
        StringBuilder csv = new StringBuilder();
        csv.append("id,name,email,phone,role,isVerified,isActive,createdAt\n");
        adminService.getAllUsersForExport().forEach(u -> {
            csv.append(escapeCsv(u.getId().toString())).append(",");
            csv.append(escapeCsv(u.getName())).append(",");
            csv.append(escapeCsv(u.getEmail())).append(",");
            csv.append(escapeCsv(u.getPhone())).append(",");
            csv.append(escapeCsv(u.getRole() != null ? u.getRole().name() : "")).append(",");
            csv.append(u.getIsVerified() != null && u.getIsVerified() ? "true" : "false").append(",");
            csv.append(u.getIsActive() != null && u.getIsActive() ? "true" : "false").append(",");
            csv.append(u.getCreatedAt() != null ? u.getCreatedAt().toString() : "").append("\n");
        });
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "users_export.csv");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    /**
     * Export businesses as CSV
     * GET /api/v1/admin/businesses/export
     */
    @GetMapping(value = "/businesses/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportBusinesses() {
        StringBuilder csv = new StringBuilder();
        csv.append("id,name,category,region,district,status,isVerified,createdAt\n");
        adminService.getAllBusinessesForExport().forEach(b -> {
            csv.append(escapeCsv(b.getId().toString())).append(",");
            csv.append(escapeCsv(b.getName())).append(",");
            csv.append(escapeCsv(b.getCategory())).append(",");
            csv.append(escapeCsv(b.getRegion())).append(",");
            csv.append(escapeCsv(b.getDistrict())).append(",");
            csv.append(escapeCsv(b.getStatus() != null ? b.getStatus().name() : "")).append(",");
            csv.append(b.getIsVerified() != null && b.getIsVerified() ? "true" : "false").append(",");
            csv.append(b.getCreatedAt() != null ? b.getCreatedAt().toString() : "").append("\n");
        });
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "businesses_export.csv");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
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
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search) {
        PagedResponse<UserResponse> users = adminService.getAllUsers(page, size, role, isActive, search);
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

    /**
     * Impersonate user – get tokens to access app as that user
     * POST /api/v1/admin/users/{id}/impersonate
     */
    @PostMapping("/users/{id}/impersonate")
    public ResponseEntity<ApiResponse<com.wakilfly.dto.response.AuthResponse>> impersonateUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        com.wakilfly.dto.response.AuthResponse auth = adminService.impersonateUser(id, adminId);
        return ResponseEntity.ok(ApiResponse.success("Use tokens to access as user", auth));
    }

    // ==================== ORDERS (ADMIN) ====================

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<com.wakilfly.dto.response.PagedResponse<com.wakilfly.dto.response.OrderResponse>>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        com.wakilfly.dto.response.PagedResponse<com.wakilfly.dto.response.OrderResponse> orders = adminService.getAllOrders(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<com.wakilfly.dto.response.OrderResponse>> getOrderById(@PathVariable UUID id) {
        com.wakilfly.dto.response.OrderResponse order = adminService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<com.wakilfly.dto.response.OrderResponse>> updateOrderStatus(
            @PathVariable UUID id,
            @RequestBody com.wakilfly.dto.request.UpdateOrderStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        com.wakilfly.dto.response.OrderResponse order = adminService.updateOrderStatus(id, adminId, request);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", order));
    }

    // ==================== PRODUCTS (ADMIN) ====================

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<com.wakilfly.dto.response.PagedResponse<com.wakilfly.dto.response.ProductResponse>>> getAllProducts(
            @RequestParam(required = false) UUID businessId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        com.wakilfly.dto.response.PagedResponse<com.wakilfly.dto.response.ProductResponse> products =
                adminService.getAllProducts(businessId, active, search, page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PutMapping("/products/{id}/active")
    public ResponseEntity<ApiResponse<com.wakilfly.dto.response.ProductResponse>> setProductActive(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isActive = (Boolean) body.getOrDefault("isActive", true);
        com.wakilfly.dto.response.ProductResponse product = adminService.setProductActive(id, isActive);
        return ResponseEntity.ok(ApiResponse.success("Product updated", product));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        adminService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted"));
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

    // ==================== SETTINGS ====================

    /**
     * Get admin settings (agent register amount, to-be-business amount)
     * GET /api/v1/admin/settings
     */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<AdminSettingsResponse>> getSettings() {
        AdminSettingsResponse settings = adminService.getSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    /**
     * Update admin settings
     * PUT /api/v1/admin/settings
     */
    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<AdminSettingsResponse>> updateSettings(
            @RequestBody AdminSettingsUpdateRequest request) {
        AdminSettingsResponse settings = adminService.updateSettings(request);
        return ResponseEntity.ok(ApiResponse.success("Settings updated", settings));
    }

    // ==================== PAYMENT MONITORING ====================

    /**
     * Get all payments with filters (for admin monitoring)
     * GET /api/v1/admin/payments
     */
    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentHistoryResponse>>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentType type,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        PagedResponse<PaymentHistoryResponse> payments = adminService.getAllPayments(
                page, size, status, type, userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    // ==================== AGENT PACKAGE MANAGEMENT ====================

    /**
     * Get all agent packages
     * GET /api/v1/admin/agent-packages
     */
    @GetMapping("/agent-packages")
    public ResponseEntity<ApiResponse<java.util.List<AgentPackageResponse>>> getAgentPackages() {
        java.util.List<AgentPackageResponse> packages = adminService.getAllAgentPackages();
        return ResponseEntity.ok(ApiResponse.success(packages));
    }

    /**
     * Create a new agent package
     * POST /api/v1/admin/agent-packages
     */
    @PostMapping("/agent-packages")
    public ResponseEntity<ApiResponse<AgentPackageResponse>> createAgentPackage(
            @RequestBody CreateAgentPackageRequest request) {
        AgentPackageResponse packageResponse = adminService.createAgentPackage(request);
        return ResponseEntity.ok(ApiResponse.success("Agent package created successfully", packageResponse));
    }

    /**
     * Update an agent package
     * PUT /api/v1/admin/agent-packages/{id}
     */
    @PutMapping("/agent-packages/{id}")
    public ResponseEntity<ApiResponse<AgentPackageResponse>> updateAgentPackage(
            @PathVariable UUID id,
            @RequestBody CreateAgentPackageRequest request) {
        AgentPackageResponse packageResponse = adminService.updateAgentPackage(id, request);
        return ResponseEntity.ok(ApiResponse.success("Agent package updated successfully", packageResponse));
    }

    /**
     * Delete an agent package
     * DELETE /api/v1/admin/agent-packages/{id}
     */
    @DeleteMapping("/agent-packages/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAgentPackage(@PathVariable UUID id) {
        adminService.deleteAgentPackage(id);
        return ResponseEntity.ok(ApiResponse.success("Agent package deleted successfully"));
    }
}
