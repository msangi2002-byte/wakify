package com.wakilfly.service;

import com.wakilfly.dto.request.ChunkCompleteRequest;
import com.wakilfly.dto.request.ChunkStartRequest;
import com.wakilfly.dto.response.ChunkCompleteResponse;
import com.wakilfly.dto.response.ChunkStartResponse;
import com.wakilfly.dto.response.ChunkUploadResponse;
import com.wakilfly.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkUploadService {

    private final FileStorageService fileStorageService;
    private final VideoThumbnailService videoThumbnailService;

    @Value("${upload.path:uploads}")
    private String uploadPath;

    /** Base path for chunk temp files – resolved at init (absolute, writable) */
    private Path chunksBasePath;

    /** Chunks folder name under upload path */
    private static final String CHUNKS_DIR = "chunks";

    /** Tracks which uploadIds have been started (for cleanup on orphan) */
    private final Set<String> activeUploads = ConcurrentHashMap.newKeySet();

    /** Upload tickets: uploadId -> metadata (from start request) */
    private final Map<String, TicketInfo> uploadTickets = new ConcurrentHashMap<>();

    private static final int MAX_TOTAL_CHUNKS = 200; // ~200MB at 1MB/chunk

    private static final class TicketInfo {
        final String filename;
        final String subdirectory;

        TicketInfo(String filename, String subdirectory) {
            this.filename = filename;
            this.subdirectory = subdirectory;
        }
    }

    @PostConstruct
    public void initChunksDirectory() {
        Path preferred = Path.of(uploadPath, CHUNKS_DIR);
        try {
            Files.createDirectories(preferred);
            if (!Files.isWritable(preferred)) {
                throw new IOException("Directory not writable: " + preferred);
            }
            chunksBasePath = preferred.toAbsolutePath();
            log.info("Chunk upload directory ready: {}", chunksBasePath);
        } catch (IOException e) {
            log.warn("Could not use {} for chunks, falling back to temp dir: {}", preferred, e.getMessage());
            Path fallback = Path.of(System.getProperty("java.io.tmpdir", "/tmp"), "wakilfy-chunks");
            try {
                Files.createDirectories(fallback);
                chunksBasePath = fallback.toAbsolutePath();
                log.info("Chunk upload directory (fallback): {}", chunksBasePath);
            } catch (IOException e2) {
                log.error("Failed to init chunk directory. Chunked upload will fail.", e2);
                throw new IllegalStateException("Cannot create chunk directory: " + e2.getMessage(), e2);
            }
        }
    }

    /**
     * Step 1: Obtain Upload Ticket. Backend generates unique uploadId.
     * Client must use this uploadId for all chunk and complete requests.
     */
    public ChunkStartResponse startUpload(ChunkStartRequest request) {
        String filename = request.getFilename();
        String subdirectory = request.getSubdirectory();
        Integer totalChunks = request.getTotalChunks();

        if (filename == null || filename.isBlank()) {
            throw new BadRequestException("filename is required");
        }
        if (subdirectory == null || subdirectory.isBlank()) {
            throw new BadRequestException("subdirectory is required");
        }
        if (totalChunks == null || totalChunks < 1) {
            throw new BadRequestException("totalChunks must be positive");
        }
        if (totalChunks > MAX_TOTAL_CHUNKS) {
            throw new BadRequestException("totalChunks exceeds maximum of " + MAX_TOTAL_CHUNKS);
        }

        String uploadId = UUID.randomUUID().toString();
        Path dir = getChunksDir(uploadId);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.error("Failed to create chunk dir for uploadId {} at {}: {}", uploadId, dir, e.getMessage(), e);
            throw new RuntimeException("Failed to prepare upload. Check server logs and upload.path/chunks directory permissions.");
        }

        uploadTickets.put(uploadId, new TicketInfo(filename, subdirectory));
        activeUploads.add(uploadId);

        log.info("Upload ticket issued: uploadId={}, filename={}, totalChunks={}", uploadId, filename, totalChunks);

        return ChunkStartResponse.builder()
                .uploadId(uploadId)
                .filename(filename)
                .totalChunks(totalChunks)
                .build();
    }

    /**
     * Save a single chunk. Chunks are stored as uploads/chunks/{uploadId}/chunk_0, chunk_1, ...
     */
    public ChunkUploadResponse saveChunk(String uploadId, int chunkIndex, int totalChunks,
                                         String filename, MultipartFile chunk) {
        validateChunkParams(uploadId, chunkIndex, totalChunks, filename, chunk);

        TicketInfo ticket = uploadTickets.get(uploadId);
        if (ticket == null) {
            log.warn("Chunk received for unknown or expired uploadId: {}", uploadId);
            throw new BadRequestException("Invalid or expired uploadId. Call POST /upload/start first.");
        }

        activeUploads.add(uploadId);
        Path dir = getChunksDir(uploadId);

        try {
            Files.createDirectories(dir);
            Path chunkFile = dir.resolve("chunk_" + chunkIndex);
            chunk.transferTo(chunkFile.toFile());

            int received = (int) Files.list(dir)
                    .filter(p -> p.getFileName().toString().startsWith("chunk_"))
                    .count();

            boolean complete = received >= totalChunks;
            log.debug("Chunk {} saved for uploadId {} ({}/{} complete)", chunkIndex, uploadId, received, totalChunks);

            return ChunkUploadResponse.builder()
                    .uploadId(uploadId)
                    .chunkIndex(chunkIndex)
                    .totalChunks(totalChunks)
                    .receivedChunks(received)
                    .complete(complete)
                    .build();

        } catch (IOException e) {
            log.error("Failed to save chunk {} for uploadId {}", chunkIndex, uploadId, e);
            throw new RuntimeException("Failed to save chunk: " + e.getMessage());
        }
    }

    /**
     * Merge all chunks, upload to storage, return final URL. Deletes temp chunks after success.
     */
    public ChunkCompleteResponse completeUpload(ChunkCompleteRequest request) {
        String uploadId = request.getUploadId();
        String filename = request.getFilename();
        String subdirectory = request.getSubdirectory();

        if (uploadId == null || uploadId.isBlank()) {
            throw new BadRequestException("uploadId is required");
        }
        if (filename == null || filename.isBlank()) {
            throw new BadRequestException("filename is required");
        }
        if (subdirectory == null || subdirectory.isBlank()) {
            throw new BadRequestException("subdirectory is required");
        }

        Path dir = getChunksDir(uploadId);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new BadRequestException("No chunks found for uploadId: " + uploadId + ". Call POST /upload/start first.");
        }

        TicketInfo ticket = uploadTickets.get(uploadId);
        if (ticket == null) {
            throw new BadRequestException("Invalid or expired uploadId. Call POST /upload/start first.");
        }

        try {
            File mergedFile = mergeChunks(dir, uploadId, filename);
            try {
                String url = fileStorageService.storeFile(mergedFile, filename, subdirectory);

                // Generate thumbnail for video
                String thumbnailUrl = null;
                if (isVideoFile(filename)) {
                    Path thumbPath = videoThumbnailService.extractThumbnail(mergedFile);
                    if (thumbPath != null) {
                        try {
                            String thumbFilename = filenameWithoutExt(filename) + "_thumb.jpg";
                            thumbnailUrl = fileStorageService.storeFile(thumbPath.toFile(), thumbFilename, subdirectory);
                        } finally {
                            try {
                                Files.deleteIfExists(thumbPath);
                            } catch (IOException ignored) {
                            }
                        }
                    }
                }

                // Cleanup temp files and ticket
                deleteChunkDir(dir);
                mergedFile.delete();
                activeUploads.remove(uploadId);
                uploadTickets.remove(uploadId);

                return ChunkCompleteResponse.builder()
                        .url(url)
                        .thumbnailUrl(thumbnailUrl)
                        .build();
            } finally {
                if (mergedFile.exists()) mergedFile.delete();
            }

        } catch (IOException e) {
            log.error("Failed to merge chunks for uploadId {}", uploadId, e);
            throw new RuntimeException("Failed to merge chunks: " + e.getMessage());
        }
    }

    /**
     * Merge chunk files in order into a single file.
     * Uses original filename extension so FFmpeg can detect video format.
     */
    private File mergeChunks(Path chunksDir, String uploadId, String originalFilename) throws IOException {
        var chunkFiles = Files.list(chunksDir)
                .filter(p -> p.getFileName().toString().startsWith("chunk_"))
                .sorted(Comparator.comparingInt(p -> {
                    String name = p.getFileName().toString();
                    return Integer.parseInt(name.substring(6)); // "chunk_" -> index
                }))
                .toList();

        if (chunkFiles.isEmpty()) {
            throw new BadRequestException("No chunk files found for uploadId: " + uploadId);
        }

        String suffix = ".tmp";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        Path mergedPath = Files.createTempFile("wakilfy-merged-", suffix);
        try (OutputStream out = new FileOutputStream(mergedPath.toFile())) {
            for (Path chunkPath : chunkFiles) {
                Files.copy(chunkPath, out);
            }
        }

        return mergedPath.toFile();
    }

    private void deleteChunkDir(Path dir) {
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            log.warn("Could not delete temp chunk file: {}", p, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Could not clean up chunk dir: {}", dir, e);
        }
    }

    private Path getChunksDir(String uploadId) {
        if (chunksBasePath == null) {
            initChunksDirectory();
        }
        return chunksBasePath.resolve(uploadId);
    }

    private static boolean isVideoFile(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".webm")
                || lower.endsWith(".m4v") || lower.endsWith(".avi");
    }

    private static String filenameWithoutExt(String filename) {
        if (filename == null || !filename.contains(".")) return filename;
        return filename.substring(0, filename.lastIndexOf('.'));
    }

    private void validateChunkParams(String uploadId, int chunkIndex, int totalChunks,
                                    String filename, MultipartFile chunk) {
        if (uploadId == null || uploadId.isBlank()) {
            throw new BadRequestException("uploadId is required");
        }
        if (chunkIndex < 0 || totalChunks < 1 || chunkIndex >= totalChunks) {
            throw new BadRequestException("Invalid chunkIndex or totalChunks");
        }
        if (chunk == null || chunk.isEmpty()) {
            throw new BadRequestException("Chunk file is required");
        }
        if (filename == null || filename.isBlank()) {
            throw new BadRequestException("filename is required");
        }
    }
}
