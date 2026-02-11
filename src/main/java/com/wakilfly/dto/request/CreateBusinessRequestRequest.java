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

    @NotBlank(message = "Agent code is required")
    private String agentCode;

    private String category;
    private String region;
    private String district;
    private String ward;
    private String street;
    private String description;
}
