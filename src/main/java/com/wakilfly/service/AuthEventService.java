package com.wakilfly.service;

import com.wakilfly.dto.request.AuthRequestContext;
import com.wakilfly.dto.response.AuthEventResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.model.AuthEvent;
import com.wakilfly.model.AuthEventType;
import com.wakilfly.model.User;
import com.wakilfly.repository.AuthEventRepository;
import com.wakilfly.util.UserAgentParser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthEventService {

    private final AuthEventRepository authEventRepository;

    /**
     * Record an auth event (registration, login, login failed) with background data.
     */
    @Transactional
    public void recordEvent(AuthEventType eventType, User user, String identifier, AuthRequestContext ctx, boolean success) {
        String ip = ctx != null ? ctx.getIpAddress() : null;
        String userAgent = ctx != null ? ctx.getUserAgent() : null;
        UserAgentParser.Parsed parsed = UserAgentParser.parse(userAgent);

        AuthEvent event = AuthEvent.builder()
                .eventType(eventType)
                .user(user)
                .identifier(identifier)
                .ipAddress(ip)
                .userAgent(userAgent)
                .deviceType(parsed.getDeviceType())
                .browser(parsed.getBrowser())
                .os(parsed.getOs())
                .acceptLanguage(ctx != null ? ctx.getAcceptLanguage() : null)
                .deviceId(ctx != null ? ctx.getDeviceId() : null)
                .timezone(ctx != null ? ctx.getTimezone() : null)
                .success(success)
                .build();
        authEventRepository.save(event);
    }

    /**
     * Login activity for user (sessions / devices) – like Facebook "Where you're logged in".
     */
    public PagedResponse<AuthEventResponse> getLoginActivity(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuthEvent> events = authEventRepository.findByUserIdAndEventTypeInOrderByCreatedAtDesc(
                userId,
                new AuthEventType[]{AuthEventType.LOGIN, AuthEventType.REGISTRATION},
                pageable);

        List<AuthEventResponse> content = events.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.<AuthEventResponse>builder()
                .content(content)
                .page(events.getNumber())
                .size(events.getSize())
                .totalElements(events.getTotalElements())
                .totalPages(events.getTotalPages())
                .last(events.isLast())
                .first(events.isFirst())
                .build();
    }

    private AuthEventResponse mapToResponse(AuthEvent e) {
        return AuthEventResponse.builder()
                .id(e.getId())
                .eventType(e.getEventType().name())
                .ipAddress(e.getIpAddress())
                .deviceType(e.getDeviceType())
                .browser(e.getBrowser())
                .os(e.getOs())
                .countryFromIp(e.getCountryFromIp())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
