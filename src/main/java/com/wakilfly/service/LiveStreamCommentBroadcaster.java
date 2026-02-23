package com.wakilfly.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wakilfly.dto.response.LiveStreamCommentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory broadcaster for live stream comments. When a comment is added,
 * all SSE subscribers for that live stream receive the new comment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveStreamCommentBroadcaster {

    private static final long SSE_TIMEOUT_MS = 60_000 * 30; // 30 minutes

    private final ObjectMapper objectMapper;

    private final java.util.Map<UUID, List<SseEmitter>> liveIdToEmitters = new java.util.concurrent.ConcurrentHashMap<>();

    public SseEmitter register(UUID liveId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        liveIdToEmitters.computeIfAbsent(liveId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(liveId, emitter));
        emitter.onTimeout(() -> remove(liveId, emitter));
        emitter.onError((e) -> remove(liveId, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            log.warn("SSE send connected failed: {}", e.getMessage());
            remove(liveId, emitter);
        }
        return emitter;
    }

    public void broadcast(UUID liveId, LiveStreamCommentResponse comment) {
        List<SseEmitter> emitters = liveIdToEmitters.get(liveId);
        if (emitters == null || emitters.isEmpty()) return;
        String json;
        try {
            json = objectMapper.writeValueAsString(comment);
        } catch (JsonProcessingException e) {
            log.warn("SSE comment serialize failed: {}", e.getMessage());
            return;
        }
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("comment").data(json));
            } catch (IOException e) {
                remove(liveId, emitter);
            }
        });
    }

    /** Broadcast viewer count to all subscribers (real-time viewer count without polling). */
    public void broadcastViewerCount(UUID liveId, int count) {
        List<SseEmitter> emitters = liveIdToEmitters.get(liveId);
        if (emitters == null || emitters.isEmpty()) return;
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("viewer_count").data(String.valueOf(count)));
            } catch (IOException e) {
                remove(liveId, emitter);
            }
        });
    }

    /** Broadcast like so all viewers and creator see "X liked" on screen. */
    public void broadcastLike(UUID liveId, UUID userId, String userName, String userProfilePic) {
        List<SseEmitter> emitters = liveIdToEmitters.get(liveId);
        if (emitters == null || emitters.isEmpty()) return;
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("userId", userId != null ? userId.toString() : null);
        payload.put("userName", userName);
        payload.put("userProfilePic", userProfilePic);
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("SSE like serialize failed: {}", e.getMessage());
            return;
        }
        String data = json;
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("like").data(data));
            } catch (IOException e) {
                remove(liveId, emitter);
            }
        });
    }

    /** Broadcast gift_sent so all viewers and creator see gift pop on screen. */
    public void broadcastGiftSent(UUID liveId, String senderName, UUID giftId, String giftName, String giftIconUrl, Integer coinValue, int quantity) {
        List<SseEmitter> emitters = liveIdToEmitters.get(liveId);
        if (emitters == null || emitters.isEmpty()) return;
        java.util.Map<String, Object> gift = new java.util.LinkedHashMap<>();
        gift.put("id", giftId != null ? giftId.toString() : null);
        gift.put("name", giftName);
        gift.put("iconUrl", giftIconUrl);
        gift.put("coinValue", coinValue);
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("senderName", senderName);
        payload.put("gift", gift);
        payload.put("quantity", quantity);
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("SSE gift_sent serialize failed: {}", e.getMessage());
            return;
        }
        String data = json;
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("gift_sent").data(data));
            } catch (IOException e) {
                remove(liveId, emitter);
            }
        });
    }

    private void remove(UUID liveId, SseEmitter emitter) {
        List<SseEmitter> list = liveIdToEmitters.get(liveId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) liveIdToEmitters.remove(liveId);
        }
    }
}
