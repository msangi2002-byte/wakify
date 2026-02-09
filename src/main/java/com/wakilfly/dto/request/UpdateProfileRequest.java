package com.wakilfly.dto.request;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    private String name;
    private String bio;
    private String profilePic;
    private String coverPic;

    // Extended Details
    private String work;
    private String education;
    private String currentCity;
    private String region;
    private String country;
    private String interests;
    private String hometown;
    private String relationshipStatus;
    private String gender;
    private LocalDate dateOfBirth;
    private String website;
}
