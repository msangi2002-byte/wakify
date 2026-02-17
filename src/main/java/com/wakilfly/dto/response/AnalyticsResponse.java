package com.wakilfly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {

    private Long dailyActiveUsers;   // DAU: distinct users with lastSeen in last 24h
    private Long monthlyActiveUsers; // MAU: distinct users with lastSeen in last 30 days
}
