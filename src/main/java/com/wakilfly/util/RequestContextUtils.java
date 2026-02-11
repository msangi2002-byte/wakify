package com.wakilfly.util;

import com.wakilfly.dto.request.AuthRequestContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Build AuthRequestContext from HTTP request (background data like Facebook/Instagram).
 */
public final class RequestContextUtils {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";
    private static final String USER_AGENT = "User-Agent";
    private static final String ACCEPT_LANGUAGE = "Accept-Language";
    private static final String X_DEVICE_ID = "X-Device-ID";
    private static final String X_TIMEZONE = "X-Timezone";

    /**
     * Extract client IP (handles proxy: X-Forwarded-For, X-Real-IP).
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String xff = request.getHeader(X_FORWARDED_FOR);
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader(X_REAL_IP);
        if (xri != null && !xri.isBlank()) return xri.trim();
        return request.getRemoteAddr();
    }

    public static AuthRequestContext fromRequest(HttpServletRequest request) {
        if (request == null) {
            return AuthRequestContext.builder().build();
        }
        return AuthRequestContext.builder()
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader(USER_AGENT))
                .acceptLanguage(request.getHeader(ACCEPT_LANGUAGE))
                .deviceId(request.getHeader(X_DEVICE_ID))
                .timezone(request.getHeader(X_TIMEZONE))
                .build();
    }
}
