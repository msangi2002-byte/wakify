package com.wakilfly.dto.request;

import com.wakilfly.model.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SendMessageRequest {

    @NotNull
    private UUID recipientId;

    /** Text content (optional when sending voice/document with mediaUrl) */
    private String content;

    /** Media URL (for image, video, voice note, document) */
    private String mediaUrl;

    /** Message type: TEXT, IMAGE, VIDEO, VOICE, DOCUMENT, etc. */
    private MessageType type;
}
