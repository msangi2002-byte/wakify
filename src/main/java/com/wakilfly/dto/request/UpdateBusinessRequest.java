package com.wakilfly.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBusinessRequest {
    @Size(max = 255)
    private String name;

    @Size(max = 2000)
    private String description;

    private String category;

    private String phone;
    private String email;
    private String website;

    // Location
    private String region;
    private String district;
    private String ward;
    private String street;
    private Double latitude;
    private Double longitude;
}
