package com.wakilfly.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkCompleteResponse {

    /** Final public URL of the merged file (e.g. https://storage.wakilfy.com/posts/xxx.mp4) */
    private String url;

    /** Thumbnail URL for videos (e.g. https://storage.wakilfy.com/posts/xxx_thumb.jpg). Null for images. */
    private String thumbnailUrl;
}
