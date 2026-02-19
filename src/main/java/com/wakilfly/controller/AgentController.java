package com.wakilfly.controller;

import com.wakilfly.dto.request.AgentRegistrationRequest;
import com.wakilfly.dto.request.BusinessActivationRequest;
import com.wakilfly.dto.request.UpdateBusinessRequestDetailsRequest;
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
     * List packages available for agent registration (authenticated users, no AGENT role required).
     * GET /api/v1/agent/registration-packages
     */
    @GetMapping("/registration-packages")
    public ResponseEntity<ApiResponse<java.util.List<AgentPackageResponse>>> getRegistrationPackages() {
        java.util.List<AgentPackageResponse> packages = agentService.getRegistrationPackages();
        return ResponseEntity.ok(ApiResponse.success(packages));
    }

    /**
     * Register as an agent (any authenticated user can become an agent).
     * If request.packageId is set: creates agent (PENDING), initiates USSD payment for package; after payment success agent becomes ACTIVE.
     * If not set: legacy flow with fixed registration fee.
     * POST /api/v1/agent/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AgentRegistrationResponse>> registerAsAgent(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AgentRegistrationRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        AgentRegistrationResponse result = agentService.registerAsAgent(userId, request);
        String message = result.getOrderId() != null
                ? "USSD push sent. Complete payment on your phone to activate your agent account."
                : "Agent registration initiated. Please complete payment.";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, result));
    }

    /**
     * Get my agent profile (any authenticated user; for polling after registration until status ACTIVE).
     * GET /api/v1/agent/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AgentResponse>> getMyAgentProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        AgentResponse agent = agentService.getAgentByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(agent));
    }

    /**
     * Get agent dashboard summary (wallet, earnings, stats). Allowed for any authenticated user so dashboard loads after registration redirect.
     * GET /api/v1/agent/dashboard
     */
    @GetMapping("/dashboard")
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
     * Approve/Verify a business activation manually
     * POST /api/v1/agent/businesses/{id}/approve
     */
    @PostMapping("/businesses/{id}/approve")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<BusinessResponse>> approveBusiness(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        BusinessResponse business = agentService.approveBusiness(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Business approved successfully", business));
    }

    /**
     * Cancel a business activation
     * DELETE /api/v1/agent/businesses/{id}
     */
    @DeleteMapping("/businesses/{id}")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<Void>> cancelBusiness(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        agentService.cancelBusiness(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Business activation cancelled successfully"));
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
     * Get one business request detail (for agent: map, distance, user details, documents).
     * GET /api/v1/agent/business-requests/{id}
     */
    @GetMapping("/business-requests/{id}")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<BusinessRequestResponse>> getBusinessRequestById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        BusinessRequestResponse request = businessRequestService.getByIdForAgent(id, userId);
        return ResponseEntity.ok(ApiResponse.success(request));
    }

    /**
     * Agent updates document/details for a business request (NIDA, TIN, company, ID doc URLs).
     * PATCH /api/v1/agent/business-requests/{id}
     */
    @PatchMapping("/business-requests/{id}")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<BusinessRequestResponse>> updateBusinessRequestDetails(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateBusinessRequestDetailsRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        BusinessRequestResponse updated = businessRequestService.updateRequestDetailsByAgent(
                id, userId,
                request.getNidaNumber(), request.getTinNumber(), request.getCompanyName(),
                request.getIdDocumentUrl(), request.getIdBackDocumentUrl());
        return ResponseEntity.ok(ApiResponse.success("Details updated", updated));
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

    /**
     * List agents for "Become a business" (authenticated).
     * sort: popularity (default), rating, nearby. For nearby pass lat & lng.
     * GET /api/v1/agent/for-business-request
     */
    @GetMapping("/for-business-request")
    public ResponseEntity<ApiResponse<PagedResponse<AgentResponse>>> getAgentsForBusinessRequest(
            @RequestParam(required = false, defaultValue = "popularity") String sort,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<AgentResponse> agents = agentService.getAgentsForBusinessRequest(sort, lat, lng, page, size);
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
     * Legacy/alternate webhook for payment confirmation.
     * Full path: POST /api/v1/agent/webhooks/payment-callback (expects body: transactionId, status).
     * For HarakaPay use PaymentWebhookController: POST /api/v1/webhooks/harakapay (expects order_id).
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

    // ==================== AGENT PACKAGE MANAGEMENT ====================

    /**
     * Get all available agent packages
     * GET /api/v1/agent/packages
     */
    @GetMapping("/packages")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<java.util.List<AgentPackageResponse>>> getAvailablePackages() {
        java.util.List<AgentPackageResponse> packages = agentService.getAvailablePackages();
        return ResponseEntity.ok(ApiResponse.success(packages));
    }

    /**
     * Initiate package purchase/upgrade payment
     * POST /api/v1/agent/packages/{packageId}/purchase
     */
    @PostMapping("/packages/{packageId}/purchase")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> purchasePackage(
            @PathVariable UUID packageId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        String paymentPhone = request.get("paymentPhone");
        
        if (paymentPhone == null || paymentPhone.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Payment phone number is required"));
        }

        String orderId = agentService.initiatePackagePurchase(userId, packageId, paymentPhone.trim());
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("orderId", orderId);
        response.put("message", "USSD push imetumwa kwa simu yako. Fuata maelekezo kukamilisha malipo.");
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment initiated", response));
    }
}
