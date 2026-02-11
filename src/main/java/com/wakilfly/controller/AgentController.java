package com.wakilfly.controller;

import com.wakilfly.dto.request.AgentRegistrationRequest;
import com.wakilfly.dto.request.BusinessActivationRequest;
import com.wakilfly.dto.request.WithdrawalRequest;
import com.wakilfly.dto.response.*;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.AgentService;
import com.wakilfly.service.BusinessRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final BusinessRequestService businessRequestService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Register as an agent (any authenticated user can become an agent)
     * POST /api/v1/agent/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AgentResponse>> registerAsAgent(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AgentRegistrationRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        AgentResponse agent = agentService.registerAsAgent(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Agent registration initiated. Please complete payment.", agent));
    }

    /**
     * Get my agent profile
     * GET /api/v1/agent/me
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<AgentResponse>> getMyAgentProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        AgentResponse agent = agentService.getAgentByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(agent));
    }

    /**
     * Get agent dashboard summary (wallet, earnings, stats)
     * GET /api/v1/agent/dashboard
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<AgentDashboardResponse>> getAgentDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        AgentDashboardResponse dashboard = agentService.getAgentDashboard(userId);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    /**
     * Get agent by code (public)
     * GET /api/v1/agent/code/{agentCode}
     */
    @GetMapping("/code/{agentCode}")
    public ResponseEntity<ApiResponse<AgentResponse>> getAgentByCode(@PathVariable String agentCode) {
        AgentResponse agent = agentService.getAgentByCode(agentCode);
        return ResponseEntity.ok(ApiResponse.success(agent));
    }

    /**
     * Activate a business (agent only)
     * POST /api/v1/agent/activate-business
     */
    @PostMapping("/activate-business")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<BusinessResponse>> activateBusiness(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BusinessActivationRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        BusinessResponse business = agentService.activateBusiness(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Business activation initiated. Awaiting payment confirmation.", business));
    }

    /**
     * Get my activated businesses
     * GET /api/v1/agent/businesses
     */
    @GetMapping("/businesses")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<BusinessResponse>>> getMyBusinesses(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<BusinessResponse> businesses = agentService.getAgentBusinesses(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(businesses));
    }

    /**
     * Get business requests (users who selected this agent when requesting to become a business).
     * GET /api/v1/agent/business-requests
     */
    @GetMapping("/business-requests")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<BusinessRequestResponse>>> getBusinessRequests(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<BusinessRequestResponse> requests = businessRequestService.findByAgentId(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    /**
     * Get my commissions
     * GET /api/v1/agent/commissions
     */
    @GetMapping("/commissions")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<CommissionResponse>>> getMyCommissions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<CommissionResponse> commissions = agentService.getAgentCommissions(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(commissions));
    }

    /**
     * Search agents (public)
     * GET /api/v1/agent/search
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<AgentResponse>>> searchAgents(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<AgentResponse> agents = agentService.searchAgents(q, page, size);
        return ResponseEntity.ok(ApiResponse.success(agents));
    }

    // ==================== WITHDRAWAL ENDPOINTS ====================

    /**
     * Request a withdrawal
     * POST /api/v1/agent/withdrawals
     */
    @PostMapping("/withdrawals")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<WithdrawalResponse>> requestWithdrawal(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WithdrawalRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        WithdrawalResponse withdrawal = agentService.requestWithdrawal(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Withdrawal request submitted", withdrawal));
    }

    /**
     * Get my withdrawal history
     * GET /api/v1/agent/withdrawals
     */
    @GetMapping("/withdrawals")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<WithdrawalResponse>>> getWithdrawalHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<WithdrawalResponse> withdrawals = agentService.getWithdrawalHistory(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(withdrawals));
    }

    /**
     * Cancel a pending withdrawal
     * DELETE /api/v1/agent/withdrawals/{id}
     */
    @DeleteMapping("/withdrawals/{id}")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<Void>> cancelWithdrawal(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        agentService.cancelWithdrawal(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal cancelled"));
    }

    /**
     * Webhook for payment confirmation
     * POST /api/v1/webhooks/payment-callback
     */
    @PostMapping("/webhooks/payment-callback")
    public ResponseEntity<ApiResponse<Void>> paymentCallback(@RequestBody Map<String, Object> payload) {
        // TODO: Validate webhook signature
        String transactionId = (String) payload.get("transactionId");
        String status = (String) payload.get("status");

        if ("SUCCESS".equalsIgnoreCase(status) && transactionId != null) {
            agentService.confirmPayment(transactionId);
        }

        return ResponseEntity.ok(ApiResponse.success("Callback received"));
    }
}
