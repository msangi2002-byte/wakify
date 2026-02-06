package com.wakilfly.controller;

import com.wakilfly.dto.request.CreateReportRequest;
import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.ReportResponse;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Submit a report
     * POST /api/v1/reports
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateReportRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        ReportResponse report = reportService.createReport(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report submitted. We will review it shortly.", report));
    }
}
