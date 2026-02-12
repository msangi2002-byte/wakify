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
public class BusinessActivationRequest {

    @NotBlank(message = "Business name is required")
    private String businessName;

    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    // Owner details (can be new user or existing)
    private UUID ownerId; // If existing user

    // If new user needs to be created
    private String ownerName;
    private String ownerPhone;
    private String ownerEmail;
    /** Password for the new owner so they can log in after payment success. */
    private String ownerPassword;

    // Location
    @NotBlank(message = "Region is required")
    private String region;

    @NotBlank(message = "District is required")
    private String district;

    private String ward;
    private String street;

    // Payment details (for 10,000/= activation fee)
    @NotBlank(message = "Payment phone is required")
    private String paymentPhone;
}
