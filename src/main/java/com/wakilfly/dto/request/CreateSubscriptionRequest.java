package com.wakilfly.dto.request;

import com.wakilfly.entity.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionRequest {

    @NotNull(message = "Subscription plan is required")
    private SubscriptionPlan plan;

    private Boolean autoRenew;

    @NotNull(message = "Payment phone is required")
    private String paymentPhone;
}
