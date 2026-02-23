package com.wakilfly.dto.request;

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
public class CreateDraftOrderRequest {

    @NotNull(message = "Inquiry ID is required")
    private UUID inquiryId;

    private String deliveryName;
    private String deliveryPhone;
    private String deliveryAddress;
    private String deliveryRegion;
    private String deliveryDistrict;
    private String customerNotes;
}
