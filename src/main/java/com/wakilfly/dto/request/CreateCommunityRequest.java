package com.wakilfly.dto.request;

import com.wakilfly.model.CommunityType;
import com.wakilfly.model.Visibility;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCommunityRequest {

    @NotBlank
    private String name;

    private String description;

    private CommunityType type = CommunityType.GROUP;

    private Visibility privacy = Visibility.PUBLIC;

    // Optional cover image handled via MultipartFile in Controller
}
