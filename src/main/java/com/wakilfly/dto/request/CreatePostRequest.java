package com.wakilfly.dto.request;

import com.wakilfly.model.PostType;
import com.wakilfly.model.Visibility;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {

    @Size(max = 2000, message = "Caption cannot exceed 2000 characters")
    private String caption;

    private Visibility visibility = Visibility.PUBLIC;

    private PostType postType = PostType.POST;

    private List<UUID> productTags;
}
