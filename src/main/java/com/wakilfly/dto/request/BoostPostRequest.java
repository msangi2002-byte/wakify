package com.wakilfly.dto.request;

import com.wakilfly.model.PromotionObjective;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Extended Boost Post request – objective, audience, budget.
 * Map to Promotion: objective, targetRegions, targetAgeMin/Max, targetGender.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoostPostRequest {

    private String postId;
    private long targetReach;
    private String paymentPhone;

    /** Objective: ENGAGEMENT, MESSAGES, TRAFFIC (Website visits). */
    private String objective;

    /** Audience: AUTOMATIC (followers + similar), LOCAL (location+age+interest), CUSTOM (future: pixel). */
    private String audienceType;

    /** For LOCAL audience: regions to target (e.g. "Dar es Salaam", "Mwanza"). */
    private List<String> targetRegions;
    private Integer targetAgeMin;
    private Integer targetAgeMax;
    /** MALE, FEMALE, ALL */
    private String targetGender;
}
