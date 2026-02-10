package com.wakilfly.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Proxies WHIP/WHEP requests to SRS to avoid CORS.
 * Browser cannot fetch streaming.wakilfy.com directly; backend forwards the request.
 */
@RestController
@RequestMapping("/api/v1/streaming")
public class StreamingProxyController {

    private static final Logger log = LoggerFactory.getLogger(StreamingProxyController.class);

    private final RestTemplate restTemplate;

    @Value("${streaming.srs-base-url:https://streaming.wakilfy.com}")
    private String srsBaseUrl;

    public StreamingProxyController() {
        this.restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(15000);
        restTemplate.setRequestFactory(factory);
    }

    @PostMapping("/whip")
    public ResponseEntity<String> whip(
            @RequestParam(defaultValue = "live") String app,
            @RequestParam String stream,
            @RequestBody String sdp) {
        String url = UriComponentsBuilder.fromHttpUrl(srsBaseUrl + "/rtc/v1/whip/")
                .queryParam("app", app).queryParam("stream", stream).toUriString();
        return proxyPost("WHIP", url, stream, sdp);
    }

    @PostMapping("/whep")
    public ResponseEntity<String> whep(
            @RequestParam(defaultValue = "live") String app,
            @RequestParam String stream,
            @RequestBody String sdp) {
        String url = UriComponentsBuilder.fromHttpUrl(srsBaseUrl + "/rtc/v1/whep/")
                .queryParam("app", app).queryParam("stream", stream).toUriString();
        return proxyPost("WHEP", url, stream, sdp);
    }

    private ResponseEntity<String> proxyPost(String type, String url, String stream, String body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/sdp"));
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String responseBody = res.getBody() != null ? res.getBody() : "";
            return ResponseEntity.status(res.getStatusCode())
                    .contentType(MediaType.parseMediaType("application/sdp"))
                    .body(responseBody);
        } catch (HttpStatusCodeException e) {
            log.warn("{} failed stream={} status={} body={}", type, stream, e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(type + " failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("{} failed stream={} url={} error={}", type, stream, url, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(type + " proxy error: cannot reach SRS. " + e.getMessage());
        }
    }
}
