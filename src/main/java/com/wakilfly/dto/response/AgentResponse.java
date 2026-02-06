package com.wakilfly.dto.response;

import com.wakilfly.entity.AgentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {

    private UUID id;
    private UUID userId;
    private String name;
    private String phone;
    private String email;
    private String profilePic;

    private String agentCode;
    private String nationalId;
    private AgentStatus status;
    private Boolean isVerified;

    private String region;
    private String district;
    private String ward;

    private BigDecimal totalEarnings;
    private BigDecimal availableBalance;

    private Integer businessesActivated;
    private Integer totalReferrals;

    private LocalDateTime registeredAt;
    private LocalDateTime approvedAt;
}
