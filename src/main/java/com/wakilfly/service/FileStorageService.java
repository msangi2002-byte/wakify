package com.wakilfly.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${upload.path:uploads}")
    private String uploadPath;

    public String storeFile(MultipartFile file, String subdirectory) {
        try {
            // Create directory if not exists
            Path uploadDir = Paths.get(uploadPath, subdirectory);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // Save file
            Path targetLocation = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored: {}", targetLocation);

            // Return relative URL
            return "/" + uploadPath + "/" + subdirectory + "/" + filename;
        } catch (IOException ex) {
            log.error("Failed to store file", ex);
            throw new RuntimeException("Failed to store file", ex);
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            if (fileUrl != null && fileUrl.startsWith("/")) {
                Path filePath = Paths.get(fileUrl.substring(1));
                Files.deleteIfExists(filePath);
                log.info("File deleted: {}", filePath);
            }
        } catch (IOException ex) {
            log.error("Failed to delete file", ex);
        }
    }
}
