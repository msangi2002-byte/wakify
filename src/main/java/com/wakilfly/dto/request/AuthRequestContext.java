package com.wakilfly.dto.request;

import lombok.Builder;
import lombok.Data;

/**
 * Background data from the request (IP, device, browser) – like Facebook/Instagram collect.
 * Built in controller from HttpServletRequest and optional headers.
 */
@Data
@Builder
public class AuthRequestContext {

    private String ipAddress;
    private String userAgent;
    private String acceptLanguage;
    private String deviceId;   // Optional: client can send X-Device-ID
    private String timezone;  // Optional: client can send X-Timezone, e.g. Africa/Dar_es_Salaam
}
