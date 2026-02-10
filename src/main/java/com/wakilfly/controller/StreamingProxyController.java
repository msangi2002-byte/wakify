package com.wakilfly.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Proxies WHIP/WHEP requests to SRS to avoid CORS.
 * Browser cannot fetch streaming.wakilfy.com directly; backend forwards the request.
 */
@RestController
@RequestMapping("/api/v1/streaming")
public class StreamingProxyController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${streaming.srs-base-url:https://streaming.wakilfy.com}")
    private String srsBaseUrl;

    @PostMapping("/whip")
    public ResponseEntity<String> whip(
            @RequestParam(defaultValue = "live") String app,
            @RequestParam String stream,
            @RequestBody String sdp) {
        String url = UriComponentsBuilder.fromHttpUrl(srsBaseUrl + "/rtc/v1/whip/")
                .queryParam("app", app).queryParam("stream", stream).toUriString();
        return proxyPost(url, sdp);
    }

    @PostMapping("/whep")
    public ResponseEntity<String> whep(
            @RequestParam(defaultValue = "live") String app,
            @RequestParam String stream,
            @RequestBody String sdp) {
        String url = UriComponentsBuilder.fromHttpUrl(srsBaseUrl + "/rtc/v1/whep/")
                .queryParam("app", app).queryParam("stream", stream).toUriString();
        return proxyPost(url, sdp);
    }

    private ResponseEntity<String> proxyPost(String url, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/sdp"));
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        String responseBody = res.getBody() != null ? res.getBody() : "";
        return ResponseEntity.status(res.getStatusCode())
                .contentType(MediaType.parseMediaType("application/sdp"))
                .body(responseBody);
    }
}
