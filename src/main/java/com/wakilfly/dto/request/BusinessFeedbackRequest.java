package com.wakilfly.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BusinessFeedbackRequest {

    @NotBlank(message = "Feedback content is required")
    @Size(max = 2000)
    private String content;
}
