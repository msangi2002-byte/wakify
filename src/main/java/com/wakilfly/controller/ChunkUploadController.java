package com.wakilfly.controller;

import com.wakilfly.dto.request.ChunkCompleteRequest;
import com.wakilfly.dto.request.ChunkStartRequest;
import com.wakilfly.dto.response.ApiResponse;
import com.wakilfly.dto.response.ChunkCompleteResponse;
import com.wakilfly.dto.response.ChunkStartResponse;
import com.wakilfly.dto.response.ChunkUploadResponse;
import com.wakilfly.service.ChunkUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class ChunkUploadController {

    private final ChunkUploadService chunkUploadService;

    /**
     * Step 1: Obtain Upload Ticket. Backend generates unique uploadId.
     * Call this before sending chunks. Use the returned uploadId for chunk and complete.
     */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<ChunkStartResponse>> startUpload(
            @Valid @RequestBody ChunkStartRequest request) {
        ChunkStartResponse response = chunkUploadService.startUpload(request);
        return ResponseEntity.ok(ApiResponse.success("Upload ticket issued", response));
    }

    /**
     * Step 2: Upload a single chunk (e.g. 1MB or 500KB).
     * Form fields: uploadId, chunkIndex, totalChunks, filename
     * Form file: chunk
     *
     * Each request stays under Nginx limit (e.g. 1MB default) so no 413.
     */
    @PostMapping(value = "/chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ChunkUploadResponse>> uploadChunk(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam("filename") String filename,
            @RequestParam("chunk") MultipartFile chunk) {

        ChunkUploadResponse response = chunkUploadService.saveChunk(
                uploadId, chunkIndex, totalChunks, filename, chunk);

        return ResponseEntity.ok(ApiResponse.success("Chunk saved", response));
    }

    /**
     * Merge all chunks and upload to storage. Returns final public URL.
     * Call this after all chunks have been uploaded.
     */
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<ChunkCompleteResponse>> completeUpload(
            @Valid @RequestBody ChunkCompleteRequest request) {

        ChunkCompleteResponse response = chunkUploadService.completeUpload(request);
        return ResponseEntity.ok(ApiResponse.success("Upload complete", response));
    }
}