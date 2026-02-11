package com.wakilfly.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateCommunityEventRequest {

    @NotBlank
    @Size(max = 300)
    private String title;

    private String description;

    @Size(max = 500)
    private String location;

    @NotNull
    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
