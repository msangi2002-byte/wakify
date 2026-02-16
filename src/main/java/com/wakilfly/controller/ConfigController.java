package com.wakilfly.controller;

import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Public config endpoints (e.g. fee amounts for agent registration and business activation).
 */
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class ConfigController {

    private final SystemConfigService systemConfigService;

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
}
