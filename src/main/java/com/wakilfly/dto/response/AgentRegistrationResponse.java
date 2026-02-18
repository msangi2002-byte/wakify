package com.wakilfly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRegistrationResponse {

    private AgentResponse agent;
    /** Present when registration was with a package; USSD push sent, frontend can poll agent status until ACTIVE. */
    private String orderId;
}
