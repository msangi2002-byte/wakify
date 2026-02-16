package com.wakilfly.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminSettingsUpdateRequest {

    @NotNull
    @DecimalMin("0")
    private BigDecimal agentRegisterAmount;

    @NotNull
    @DecimalMin("0")
    private BigDecimal toBeBusinessAmount;

    @NotNull
    @DecimalMin("0")
    private BigDecimal adsPricePerPerson;
}
