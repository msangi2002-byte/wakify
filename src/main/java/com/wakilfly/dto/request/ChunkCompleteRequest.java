package com.wakilfly.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkCompleteRequest {

    @NotBlank(message = "uploadId is required")
    private String uploadId;

    @NotBlank(message = "filename is required")
    private String filename;

    /** Subdirectory on storage: posts, avatars, etc. */
    @NotNull(message = "subdirectory is required")
    private String subdirectory;
}
