package com.wakilfly.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class NotificationSettingsResponse {

    /** Map of notification type (e.g. LIKE, COMMENT) to enabled (true/false). Default true if not set. */
    private Map<String, Boolean> byType;
}
