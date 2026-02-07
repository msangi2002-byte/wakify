package com.wakilfly.controller;

import com.wakilfly.service.LiveStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/streams")
@RequiredArgsConstructor
@Slf4j
public class SrsWebhookController {

    private final LiveStreamService streamService;

    @Value("${srs.webhook.token:WAKILFY_SECRET}")
    private String webhookToken;

    @PostMapping("/on-publish")
    public ResponseEntity<String> onPublish(@RequestBody Map<String, Object> payload,
            @RequestParam(required = false) String token) {
        // Validate secret token
        if (token == null || !token.equals(webhookToken)) {
            log.warn("Invalid SRS webhook token received: {}", token);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("1");
        }

        String streamKey = (String) payload.get("stream");
        String ip = (String) payload.get("ip");
        log.info("SRS on_publish callback: streamKey={}, ip={}", streamKey, ip);

        try {
            boolean isValid = streamService.verifyStreamKey(streamKey);

            if (isValid) {
                // TODO: Trigger SRS transcoding API here if needed (e.g. to 360p)
                // RestTemplate or WebClient could be used to call HTTP API:
                // http://localhost:1985/api/v1/vhosts/...

                return ResponseEntity.ok("0");
            } else {
                log.warn("Stream rejected: Invalid streamKey {}", streamKey);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("1");
            }
        } catch (Exception e) {
            log.error("Error processing on-publish", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("1");
        }
    }

    @PostMapping("/on-unpublish")
    public ResponseEntity<String> onUnpublish(@RequestBody Map<String, Object> payload,
            @RequestParam(required = false) String token) {
        // Validate secret token
        if (token == null || !token.equals(webhookToken)) {
            log.warn("Invalid SRS webhook token received: {}", token);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("1");
        }

        String streamKey = (String) payload.get("stream");
        log.info("SRS on_unpublish callback: streamKey={}", streamKey);

        try {
            streamService.setStreamOffline(streamKey);
            return ResponseEntity.ok("0");
        } catch (Exception e) {
            log.error("Error processing on-unpublish", e);
            // Even if we fail to update DB, we generally return 0 to SRS unless we want to
            // keep retrying?
            // Usually best to acknowledge receipt.
            return ResponseEntity.ok("0");
        }
    }
}
