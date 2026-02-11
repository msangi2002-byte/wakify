package com.wakilfly.dto.response;

import com.wakilfly.model.BusinessRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessRequestResponse {

    private UUID id;
    private UUID userId;
    private String userName;
    private String userPhone;
    private UUID agentId;
    private String agentCode;
    private String businessName;
    private String ownerPhone;
    private String category;
    private String region;
    private String district;
    private String ward;
    private String street;
    private String description;
    private BusinessRequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
