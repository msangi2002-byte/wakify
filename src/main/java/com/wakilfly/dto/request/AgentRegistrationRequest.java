package com.wakilfly.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRegistrationRequest {

    /** When set, user registers with this package and pays package price (USSD); otherwise legacy fixed fee. */
    private UUID packageId;

    @NotBlank(message = "National ID is required")
    private String nationalId;

    @NotBlank(message = "Region is required")
    private String region;

    @NotBlank(message = "District is required")
    private String district;

    private String ward;

    private String street;

    /** Map pin (optional). Recommended: capture automatically in background (GPS) on agent registration screen and send here. */
    private Double latitude;
    private Double longitude;

    // Payment details (for 20,000/= registration fee)
    @NotBlank(message = "Phone number for payment is required")
    private String paymentPhone;
}
