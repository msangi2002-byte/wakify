package com.wakilfly.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata for chunk upload. Sent as form fields alongside the chunk file.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadRequest {

    /** Unique ID for this upload session (client-generated UUID) */
    private String uploadId;

    /** 0-based chunk index */
    private Integer chunkIndex;

    /** Total number of chunks */
    private Integer totalChunks;

    /** Original filename (e.g. video.mp4) - for extension when merging */
    private String filename;
}
