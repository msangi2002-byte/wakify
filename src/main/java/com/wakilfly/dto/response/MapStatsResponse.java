package com.wakilfly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapStatsResponse {

    /** Stats by continent: name, users, agents, businesses, total */
    private List<ContinentStat> continents;

    /** Stats by country: name, continent, users, agents, businesses, total */
    private List<CountryStat> countries;

    /** Counts by type: USER, AGENT, BUSINESS */
    private Map<String, Long> byType;

    /** Total locations (users + agents + businesses with coordinates) */
    private long total;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContinentStat {
        private String name;
        private long users;
        private long agents;
        private long businesses;
        private long total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryStat {
        private String name;
        private String continent;
        private long users;
        private long agents;
        private long businesses;
        private long total;
    }
}
