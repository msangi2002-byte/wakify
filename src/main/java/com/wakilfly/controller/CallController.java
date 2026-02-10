package com.wakilfly.controller;

import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.CallResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.model.CallType;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.service.CallService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calls")
@RequiredArgsConstructor
public class CallController {

    private final CallService callService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Initiate a call (voice or video)
     * POST /api/v1/calls/initiate
     * Body: { "receiverId": "uuid-string", "type": "VOICE" | "VIDEO" }
     */
    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<CallResponse>> initiateCall(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> request) {
        UUID callerId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();

        Object receiverIdObj = request.get("receiverId");
        if (receiverIdObj == null || receiverIdObj.toString().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("receiverId is required"));
        }
        UUID receiverId;
        try {
            receiverId = UUID.fromString(receiverIdObj.toString());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid receiverId format"));
        }

        if (callerId.equals(receiverId)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("You cannot call yourself"));
        }

        Object typeObj = request.get("type");
        if (typeObj == null || typeObj.toString().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("type is required (VOICE or VIDEO)"));
        }
        CallType type;
        try {
            type = CallType.valueOf(typeObj.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid type. Use VOICE or VIDEO"));
        }

        CallResponse call = callService.initiateCall(callerId, receiverId, type);
        return ResponseEntity.ok(ApiResponse.success("Call initiated", call));
    }

    /**
     * Answer a call
     * POST /api/v1/calls/{callId}/answer
     */
    @PostMapping("/{callId}/answer")
    public ResponseEntity<ApiResponse<CallResponse>> answerCall(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID callId) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        CallResponse call = callService.answerCall(callId, userId);
        return ResponseEntity.ok(ApiResponse.success("Call answered", call));
    }

    /**
     * Reject a call
     * POST /api/v1/calls/{callId}/reject
     */
    @PostMapping("/{callId}/reject")
    public ResponseEntity<ApiResponse<CallResponse>> rejectCall(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID callId) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        CallResponse call = callService.rejectCall(callId, userId);
        return ResponseEntity.ok(ApiResponse.success("Call rejected", call));
    }

    /**
     * End a call
     * POST /api/v1/calls/{callId}/end
     */
    @PostMapping("/{callId}/end")
    public ResponseEntity<ApiResponse<CallResponse>> endCall(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID callId) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        CallResponse call = callService.endCall(callId, userId);
        return ResponseEntity.ok(ApiResponse.success("Call ended", call));
    }

    /**
     * Get call history
     * GET /api/v1/calls/history
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PagedResponse<CallResponse>>> getCallHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<CallResponse> history = callService.getCallHistory(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * Get incoming calls
     * GET /api/v1/calls/incoming
     */
    @GetMapping("/incoming")
    public ResponseEntity<ApiResponse<List<CallResponse>>> getIncomingCalls(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        List<CallResponse> calls = callService.getIncomingCalls(userId);
        return ResponseEntity.ok(ApiResponse.success(calls));
    }
}
