package com.wakilfly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapLocationResponse {

    private UUID id;
    private String name;
    private Double latitude;
    private Double longitude;
    /** USER, AGENT, or BUSINESS — use for map icon (e.g. user icon, agent icon, business icon). */
    private String type;
    private String region;
    private String category;
}
