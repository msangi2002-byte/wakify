package com.wakilfly.dto.request;

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
public class UpdatePostRequest {

    @Size(max = 2000, message = "Caption cannot exceed 2000 characters")
    private String caption;

    private Visibility visibility;

    /** Location/place (e.g. "Dar es Salaam") */
    @Size(max = 500)
    private String location;

    /** Feeling or activity (e.g. "Feeling happy") */
    @Size(max = 200)
    private String feelingActivity;

    /** Replace tagged users with this list (null = leave unchanged) */
    private List<UUID> taggedUserIds;

    /**
     * Replace media with these URLs (from chunked upload).
     * If provided, existing media may be replaced. Empty list = clear media.
     * Null = leave media unchanged.
     */
    private List<String> mediaUrls;
}
