package com.wakilfly.util;

import lombok.Builder;
import lombok.Data;

/**
 * Simple User-Agent parsing for device type, browser, OS (background data like Facebook/Instagram).
 * Does not require external library.
 */
public final class UserAgentParser {

    @Data
    @Builder
    public static class Parsed {
        private String deviceType;
        private String browser;
        private String os;
    }

    public static Parsed parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return Parsed.builder()
                    .deviceType("Unknown")
                    .browser("Unknown")
                    .os("Unknown")
                    .build();
        }
        String ua = userAgent.toLowerCase();

        // Device type: Mobile, Tablet, Desktop
        String deviceType = "Desktop";
        if (ua.contains("mobile") && !ua.contains("ipad")) deviceType = "Mobile";
        else if (ua.contains("tablet") || ua.contains("ipad")) deviceType = "Tablet";

        // OS
        String os = "Unknown";
        if (ua.contains("windows nt 10")) os = "Windows 10/11";
        else if (ua.contains("windows nt")) os = "Windows";
        else if (ua.contains("mac os x") || ua.contains("macintosh")) os = "macOS";
        else if (ua.contains("android")) os = "Android";
        else if (ua.contains("iphone") || ua.contains("ipad")) os = "iOS";
        else if (ua.contains("linux")) os = "Linux";

        // Browser
        String browser = "Unknown";
        if (ua.contains("edg/")) browser = "Edge";
        else if (ua.contains("chrome") && !ua.contains("chromium")) browser = "Chrome";
        else if (ua.contains("safari") && !ua.contains("chrome")) browser = "Safari";
        else if (ua.contains("firefox")) browser = "Firefox";
        else if (ua.contains("opera") || ua.contains("opr/")) browser = "Opera";

        return Parsed.builder()
                .deviceType(deviceType)
                .browser(browser)
                .os(os)
                .build();
    }
}
