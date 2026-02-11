package com.wakilfly.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateCommunityPollRequest {

    @NotBlank
    @Size(max = 500)
    private String question;

    private LocalDateTime endsAt;

    @NotEmpty(message = "At least 2 options required")
    @Size(min = 2, max = 10)
    private List<String> options;
}
