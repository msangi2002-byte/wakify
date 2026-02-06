package com.wakilfly.dto.request;

import com.wakilfly.entity.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotNull(message = "Recipient is required")
    private UUID recipientId;

    private String content;

    private MessageType type = MessageType.TEXT;

    private String mediaUrl;
}
