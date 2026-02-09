package com.wakilfly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadResponse {

    /** uploadId for tracking */
    private String uploadId;

    /** Chunk index that was saved */
    private int chunkIndex;

    /** Total chunks expected */
    private int totalChunks;

    /** Number of chunks received so far */
    private int receivedChunks;

    /** Whether all chunks are received and ready for complete */
    private boolean complete;
}
