package com.wakilfly.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row in "Login activity" / "Where you're logged in" (like Facebook/Instagram).
 */
@Data
@Builder
public class AuthEventResponse {

    private UUID id;
    private String eventType;  // REGISTRATION, LOGIN
    private String ipAddress;
    private String deviceType; // Mobile, Desktop, Tablet
    private String browser;
    private String os;
    private String countryFromIp;
    private LocalDateTime createdAt;
}
