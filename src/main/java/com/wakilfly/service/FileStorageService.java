package com.wakilfly.service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.wakilfly.config.StorageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final StorageConfig storageConfig;

    public FileStorageService(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;
    }

    public String storeFile(MultipartFile file, String subdirectory) {
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(storageConfig.getUsername(), storageConfig.getHost(), storageConfig.getPort());
            session.setPassword(storageConfig.getPassword());
            session.setConfig("StrictHostKeyChecking", "no"); // For development/simplicity
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;

            // Base upload path
            String currentPath = storageConfig.getUploadPath();
            // Ensure base path exists or navigate to it
            try {
                channelSftp.cd(currentPath);
            } catch (SftpException e) {
                // If base path doesn't exist, we might need to create it or fail.
                // Base path: /var/www/wakilfy-media/uploads/ (must match Nginx on storage server)
                // But for safety, let's try to create or just log
                log.warn("Base upload path might not exist: {}", currentPath);
                throw new RuntimeException("Base upload directory does not exist on storage server");
            }

            // Handle subdirectory
            if (subdirectory != null && !subdirectory.isEmpty()) {
                try {
                    channelSftp.cd(subdirectory);
                } catch (SftpException e) {
                    channelSftp.mkdir(subdirectory);
                    channelSftp.cd(subdirectory);
                }
            }

            // Generate filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // Upload
            try (InputStream inputStream = file.getInputStream()) {
                channelSftp.put(inputStream, filename);
            }

            // Construct Public URL
            // URL Structure: https://storage.wakilfy.com/ + subdirectory + / + filename
            // Note: Nginx maps /var/www/wakilfy-media/uploads/ to https://storage.wakilfy.com/
            // So if we are in subdirectory 'images', file is at .../images/filename

            StringBuilder fileUrl = new StringBuilder(storageConfig.getBaseUrl());
            if (!storageConfig.getBaseUrl().endsWith("/")) {
                fileUrl.append("/");
            }
            if (subdirectory != null && !subdirectory.isEmpty()) {
                fileUrl.append(subdirectory).append("/");
            }
            fileUrl.append(filename);

            log.info("File uploaded to VPS: {}", fileUrl);
            return fileUrl.toString();

        } catch (JSchException | SftpException | IOException e) {
            log.error("Failed to store file on VPS", e);
            throw new RuntimeException("Failed to store file on VPS", e);
        } finally {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(storageConfig.getBaseUrl())) {
            log.warn("Invalid file URL for deletion: {}", fileUrl);
            return;
        }

        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            // Extract relative path from URL
            // e.g. https://storage.wakilfy.com/avatars/abc.jpg -> avatars/abc.jpg
            String relativePath = fileUrl.substring(storageConfig.getBaseUrl().length());
            if (relativePath.startsWith("/"))
                relativePath = relativePath.substring(1);

            // Full path on server
            String fullPath = storageConfig.getUploadPath();
            if (!fullPath.endsWith("/"))
                fullPath += "/";
            fullPath += relativePath;

            JSch jsch = new JSch();
            session = jsch.getSession(storageConfig.getUsername(), storageConfig.getHost(), storageConfig.getPort());
            session.setPassword(storageConfig.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;

            channelSftp.rm(fullPath);
            log.info("File deleted from VPS: {}", fullPath);

        } catch (JSchException | SftpException e) {
            log.error("Failed to delete file from VPS: " + fileUrl, e);
        } finally {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    public void executeCommand(String command) {
        Session session = null;
        ChannelExec channelExec = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(storageConfig.getUsername(), storageConfig.getHost(), storageConfig.getPort());
            session.setPassword(storageConfig.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            Channel channel = session.openChannel("exec");
            channelExec = (ChannelExec) channel;
            channelExec.setCommand(command);

            // Should consume input stream to avoid blocking
            InputStream in = channel.getInputStream();
            channelExec.connect();

            // Read output (optional, but good for debugging/logging)
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0)
                        break;
                    log.debug("Remote command output: {}", new String(tmp, 0, i));
                }
                if (channelExec.isClosed()) {
                    if (in.available() > 0)
                        continue;
                    log.info("exit-status: " + channelExec.getExitStatus());
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }

            log.info("Remote command executed: {}", command);

        } catch (JSchException | IOException e) {
            log.error("Failed to execute remote command: " + command, e);
            throw new RuntimeException("Failed to execute remote command", e);
        } finally {
            if (channelExec != null && channelExec.isConnected()) {
                channelExec.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}
