package com.wakilfly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Admin Audience Analytics – aggregates for targeting & promotions.
 * By Interests, Location, Demographics, Behaviors.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudienceAnalyticsResponse {

    /** By interest (normalized): interest name -> count */
    private List<InterestStat> byInterests;

    /** By country: country -> count */
    private List<LocationStat> byCountry;

    /** By region: region -> count */
    private List<LocationStat> byRegion;

    /** By city: city -> count */
    private List<LocationStat> byCity;

    /** By age band: "18-24" -> count, "25-34" -> count, etc. */
    private List<DemographicStat> byAgeBand;

    /** By gender: Male -> count, Female -> count, Other -> count */
    private List<DemographicStat> byGender;

    /** Behavior segments: Online shoppers, Engaged (reactions/comments) */
    private List<BehaviorStat> byBehaviors;

    private long totalUsers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterestStat {
        private String interest;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationStat {
        private String name;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DemographicStat {
        private String bucket;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BehaviorStat {
        private String behavior;
        private long count;
    }
}
