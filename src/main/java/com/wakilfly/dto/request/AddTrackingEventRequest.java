package com.wakilfly.dto.request;

import com.wakilfly.model.TrackingEventType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddTrackingEventRequest {

    @NotNull(message = "Event type is required")
    private TrackingEventType eventType;

    private String note;

    private Double latitude;
    private Double longitude;
}
