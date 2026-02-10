package com.wakilfly.service;

import com.wakilfly.dto.response.CallResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.*;
import com.wakilfly.repository.CallRepository;
import com.wakilfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallService {

    private final CallRepository callRepository;
    private final UserRepository userRepository;

    /**
     * Initiate a call (voice or video)
     */
    @Transactional
    public CallResponse initiateCall(UUID callerId, UUID receiverId, CallType type) {
        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> new ResourceNotFoundException("Caller not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        // Block only if receiver has ONGOING call. RINGING (unanswered) is OK - caller should end it first.
        List<Call> ongoingCalls = callRepository.findByUserIdAndStatus(receiverId, CallStatus.ONGOING);
        if (!ongoingCalls.isEmpty()) {
            throw new BadRequestException("User is busy on another call");
        }

        String roomId = "call_" + UUID.randomUUID().toString().substring(0, 8);

        Call call = Call.builder()
                .caller(caller)
                .receiver(receiver)
                .type(type)
                .status(CallStatus.RINGING)
                .roomId(roomId)
                .build();

        call = callRepository.save(call);
        log.info("Call initiated: {} -> {}, type: {}, room: {}",
                caller.getName(), receiver.getName(), type, roomId);

        return mapToCallResponse(call);
    }

    /**
     * Answer a call
     */
    @Transactional
    public CallResponse answerCall(UUID callId, UUID userId) {
        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found"));

        if (!call.getReceiver().getId().equals(userId)) {
            throw new BadRequestException("You cannot answer this call");
        }

        if (call.getStatus() != CallStatus.RINGING) {
            throw new BadRequestException("Call is no longer available");
        }

        call.setStatus(CallStatus.ONGOING);
        call.setStartedAt(LocalDateTime.now());
        call = callRepository.save(call);

        log.info("Call answered: {}", callId);
        return mapToCallResponse(call);
    }

    /**
     * Reject a call
     */
    @Transactional
    public CallResponse rejectCall(UUID callId, UUID userId) {
        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found"));

        if (!call.getReceiver().getId().equals(userId)) {
            throw new BadRequestException("You cannot reject this call");
        }

        call.setStatus(CallStatus.REJECTED);
        call.setEndedAt(LocalDateTime.now());
        call = callRepository.save(call);

        log.info("Call rejected: {}", callId);
        return mapToCallResponse(call);
    }

    /**
     * End a call
     */
    @Transactional
    public CallResponse endCall(UUID callId, UUID userId) {
        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found"));

        if (!call.getCaller().getId().equals(userId) && !call.getReceiver().getId().equals(userId)) {
            throw new BadRequestException("You are not part of this call");
        }

        call.setStatus(CallStatus.ENDED);
        call.setEndedAt(LocalDateTime.now());

        if (call.getStartedAt() != null) {
            long seconds = Duration.between(call.getStartedAt(), call.getEndedAt()).getSeconds();
            call.setDurationSeconds((int) seconds);
        }

        call = callRepository.save(call);
        log.info("Call ended: {}, duration: {}s", callId, call.getDurationSeconds());
        return mapToCallResponse(call);
    }

    /**
     * Get call history
     */
    public PagedResponse<CallResponse> getCallHistory(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Call> calls = callRepository.findByUserId(userId, pageable);

        return PagedResponse.<CallResponse>builder()
                .content(calls.getContent().stream()
                        .map(this::mapToCallResponse)
                        .collect(Collectors.toList()))
                .page(calls.getNumber())
                .size(calls.getSize())
                .totalElements(calls.getTotalElements())
                .totalPages(calls.getTotalPages())
                .last(calls.isLast())
                .first(calls.isFirst())
                .build();
    }

    /**
     * Get incoming calls
     */
    public List<CallResponse> getIncomingCalls(UUID userId) {
        return callRepository.findIncomingCalls(userId).stream()
                .map(this::mapToCallResponse)
                .collect(Collectors.toList());
    }

    private CallResponse mapToCallResponse(Call call) {
        return CallResponse.builder()
                .id(call.getId())
                .caller(CallResponse.UserSummary.builder()
                        .id(call.getCaller().getId())
                        .name(call.getCaller().getName())
                        .profilePic(call.getCaller().getProfilePic())
                        .build())
                .receiver(CallResponse.UserSummary.builder()
                        .id(call.getReceiver().getId())
                        .name(call.getReceiver().getName())
                        .profilePic(call.getReceiver().getProfilePic())
                        .build())
                .type(call.getType())
                .status(call.getStatus())
                .roomId(call.getRoomId())
                .startedAt(call.getStartedAt())
                .endedAt(call.getEndedAt())
                .durationSeconds(call.getDurationSeconds())
                .createdAt(call.getCreatedAt())
                .build();
    }
}
