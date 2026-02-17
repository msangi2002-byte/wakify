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
public class TransactionReportResponse {

    private BigDecimal dailyRevenue;
    private BigDecimal weeklyRevenue;
    private BigDecimal monthlyRevenue;
    private Long dailyTransactionCount;
    private Long weeklyTransactionCount;
    private Long monthlyTransactionCount;
}
