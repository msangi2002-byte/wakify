package com.wakilfly.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Extracts a thumbnail (JPEG frame) from video files using FFmpeg.
 * Requires FFmpeg to be installed on the server (e.g. apt install ffmpeg).
 */
@Service
@Slf4j
public class VideoThumbnailService {

    /** Time in seconds to capture frame (1s avoids black first frames) */
    private static final double CAPTURE_TIME_SEC = 1.0;

    /**
     * Extract a thumbnail from the video file.
     *
     * @param videoFile The video file (e.g. .mp4, .mov, .webm)
     * @return Path to the generated JPEG thumbnail, or null if extraction fails
     */
    public Path extractThumbnail(File videoFile) {
        if (videoFile == null || !videoFile.exists() || !videoFile.isFile()) {
            log.warn("Invalid video file for thumbnail extraction: {}", videoFile);
            return null;
        }
        Path outputPath;
        try {
            outputPath = Files.createTempFile("wakilfy-video-thumb-", ".jpg");
        } catch (IOException e) {
            log.error("Failed to create temp file for thumbnail", e);
            return null;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-y",
                    "-ss", String.valueOf(CAPTURE_TIME_SEC),
                    "-i", videoFile.getAbsolutePath(),
                    "-vframes", "1",
                    "-q:v", "2",
                    "-f", "image2",
                    outputPath.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            int exit = p.waitFor();
            if (exit != 0) {
                log.warn("FFmpeg thumbnail extraction exited with {} for {}", exit, videoFile.getName());
                Files.deleteIfExists(outputPath);
                return null;
            }
            if (!Files.exists(outputPath) || Files.size(outputPath) == 0) {
                log.warn("FFmpeg produced no thumbnail for {}", videoFile.getName());
                Files.deleteIfExists(outputPath);
                return null;
            }
            return outputPath;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thumbnail extraction interrupted", e);
            deleteQuietly(outputPath);
            return null;
        } catch (IOException e) {
            log.error("Failed to extract video thumbnail for {}", videoFile.getName(), e);
            deleteQuietly(outputPath);
            return null;
        }
    }

    private static void deleteQuietly(Path p) {
        try {
            if (p != null) Files.deleteIfExists(p);
        } catch (IOException ignored) {
        }
    }
}
