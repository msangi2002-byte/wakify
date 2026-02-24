package com.wakilfly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** One item for the sidebar "Sponsored" block – from an active boost (POST promotion). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SidebarSponsoredResponse {
    private UUID id;
    private String title;
    private String description;
    private String imageUrl;
    private String targetUrl;
    private LocalDateTime createdAt;
}
