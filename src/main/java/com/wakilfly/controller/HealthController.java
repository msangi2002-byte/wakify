package com.wakilfly.controller;

import com.wakilfly.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    /**
     * Root endpoint - Health check
     * GET /
     */
    @GetMapping("/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> root() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "app", "Wakilfly API",
                "version", "1.0.0",
                "status", "running",
                "timestamp", LocalDateTime.now().toString())));
    }

    /**
     * Health check endpoint
     * GET /api/v1/health
     */
    @GetMapping("/api/v1/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "UP",
                "database", "connected",
                "timestamp", LocalDateTime.now().toString())));
    }
}
