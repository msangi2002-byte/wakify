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
    private String type; // BUSINESS
    private String region;
    private String category;
}
