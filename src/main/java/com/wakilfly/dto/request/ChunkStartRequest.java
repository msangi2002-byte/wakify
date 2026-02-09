package com.wakilfly.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to obtain an Upload Ticket before sending chunks.
 * Backend generates unique uploadId – avoids conflicts when multiple users
 * upload files with the same name (e.g. video.mp4) at the same time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkStartRequest {

    @NotBlank(message = "filename is required")
    private String filename;

    @NotNull(message = "subdirectory is required")
    private String subdirectory;

    @Positive(message = "totalChunks must be positive")
    private Integer totalChunks;
}
