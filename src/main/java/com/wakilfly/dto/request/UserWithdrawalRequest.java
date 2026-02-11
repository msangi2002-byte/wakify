package com.wakilfly.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWithdrawalRequest {

    @NotNull
    @DecimalMin(value = "1000", message = "Minimum withdrawal is 1000 TZS")
    private BigDecimal amount;

    private String paymentMethod;  // e.g. M-PESA, TIGO_PESA
    private String paymentPhone;
    private String paymentName;
}
