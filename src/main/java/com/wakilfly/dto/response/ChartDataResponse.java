package com.wakilfly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataResponse {

    private List<DayData> revenueByDay;
    private List<DayData> usersByDay;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayData {
        private String date;  // yyyy-MM-dd
        private BigDecimal value;
        private Long count;   // for users
    }
}
