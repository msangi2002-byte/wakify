package com.wakilfly.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBusinessRequestRequest {

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Owner phone is required")
    private String ownerPhone;

    /** Optional; not needed for users with an account (system handles activation via USSD). */
    private String agentCode;

    private String category;
    private String region;
    private String district;
    private String ward;
    private String street;
    private String description;
    /** User's location at request time (for agent map and distance). */
    private Double latitude;
    private Double longitude;
    /** Optional plan id; if set, payment uses plan price instead of default fee. */
    private java.util.UUID businessPlanId;
}
