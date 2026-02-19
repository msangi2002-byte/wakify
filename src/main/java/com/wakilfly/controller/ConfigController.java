package com.wakilfly.controller;

import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.BusinessRegistrationPlanResponse;
import com.wakilfly.service.BusinessRegistrationPlanService;
import com.wakilfly.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Public config endpoints (e.g. fee amounts for agent registration and business activation).
 */
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class ConfigController {

    private final SystemConfigService systemConfigService;
    private final BusinessRegistrationPlanService businessRegistrationPlanService;

    /**
     * Get fee amounts (agent register, business activation) for display in UI.
     * GET /api/v1/config/fees
     * Public – no auth required.
     */
    @GetMapping("/fees")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getFees() {
        Map<String, BigDecimal> fees = systemConfigService.getFeeAmounts();
        return ResponseEntity.ok(ApiResponse.success(fees));
    }

    /**
     * Get active business registration plans (for "Become a business" – user chooses subscription/fee).
     * GET /api/v1/config/business-registration-plans
     * Public – no auth required.
     */
    @GetMapping("/business-registration-plans")
    public ResponseEntity<ApiResponse<List<BusinessRegistrationPlanResponse>>> getBusinessRegistrationPlans() {
        List<BusinessRegistrationPlanResponse> plans = businessRegistrationPlanService.getActivePlans();
        return ResponseEntity.ok(ApiResponse.success(plans));
    }
}
