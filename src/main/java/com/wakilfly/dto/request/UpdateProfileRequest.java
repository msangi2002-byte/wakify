package com.wakilfly.dto.request;

import com.wakilfly.model.Visibility;
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
    /** Map pin (optional). Can be set from automatic location fetch or map picker. */
    private Double latitude;
    private Double longitude;
    private String interests;
    private String hometown;
    private String relationshipStatus;
    private String gender;
    private LocalDate dateOfBirth;
    private String website;
    private String language;  // Preferred language e.g. sw, en

    /** Who can see your profile: PUBLIC, FOLLOWERS, PRIVATE */
    private Visibility profileVisibility;
    /** Who can see your following/followers list: PUBLIC, FOLLOWERS, PRIVATE */
    private Visibility followingListVisibility;
}
