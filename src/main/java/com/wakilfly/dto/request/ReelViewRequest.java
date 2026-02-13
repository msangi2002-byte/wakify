package com.wakilfly.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReelViewRequest {

    /** Seconds the user watched before leaving/skipping. */
    @NotNull
    @Min(0)
    @Max(3600)
    private Integer watchTimeSeconds;

    /** True if user watched most of the reel (e.g. ≥90% of duration). */
    @NotNull
    private Boolean completed;
}
