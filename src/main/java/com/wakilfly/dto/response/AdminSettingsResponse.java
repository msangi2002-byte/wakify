package com.wakilfly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSettingsResponse {

    /** Agent registration fee (TZS). */
    private BigDecimal agentRegisterAmount;
    /** To-be-business (business activation) fee (TZS). */
    private BigDecimal toBeBusinessAmount;
}
