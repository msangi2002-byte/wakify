package com.wakilfly.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "storage.vps")
public class StorageConfig {
    private String host;
    private String username;
    private String password;
    private int port;
    private String baseUrl;
    private String uploadPath;
}
