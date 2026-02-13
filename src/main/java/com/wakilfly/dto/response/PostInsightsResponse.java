package com.wakilfly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostInsightsResponse {

    private int viewsCount;
    private double avgWatchTimeSeconds;
    private double completionRate; // 0..1
    private int likesCount;
    private int commentsCount;
    private int sharesCount;
}
