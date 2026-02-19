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
    /** Set when request is created and USSD payment is initiated; use to poll GET /payments/status/{orderId} */
    private String paymentOrderId;

    /** User location at request time (for agent map and distance). */
    private Double userLatitude;
    private Double userLongitude;
    /** Agent location (for map and distance from user). */
    private Double agentLatitude;
    private Double agentLongitude;
    /** Filled by agent when visiting: NIDA, TIN, company, ID docs. */
    private String nidaNumber;
    private String tinNumber;
    private String companyName;
    private String idDocumentUrl;
    private String idBackDocumentUrl;
}
