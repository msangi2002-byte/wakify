package com.wakilfly.dto.response;

import com.wakilfly.model.TrackingEventType;
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
public class OrderTrackingEventResponse {

    private UUID id;
    private TrackingEventType eventType;
    private String note;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
}
