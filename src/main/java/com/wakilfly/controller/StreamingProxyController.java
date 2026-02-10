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

import java.util.HashMap;
import java.util.Map;

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

    /** Optional: use this for proxy when backend and SRS are on same host (e.g. http://127.0.0.1:1985). Avoids DNS/firewall. */
    @Value("${streaming.srs-proxy-url:}")
    private String srsProxyUrl;

    public StreamingProxyController() {
        this.restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(15000);
        restTemplate.setRequestFactory(factory);
    }

    private String baseUrlForProxy() {
        return (srsProxyUrl != null && !srsProxyUrl.isBlank()) ? srsProxyUrl.trim() : srsBaseUrl;
    }

    /**
     * Diagnostic: can the backend reach SRS? GET /api/v1/streaming/health (no auth).
     * If reachable=false, fix config on the server (srs-base-url or srs-proxy-url) and firewall/DNS.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> streamingHealth() {
        String base = baseUrlForProxy();
        String checkUrl = (base.endsWith("/") ? base : base + "/") + "rtc/v1/";
        Map<String, Object> body = new HashMap<>();
        body.put("proxyTargetUrl", checkUrl);
        body.put("srsBaseUrl", srsBaseUrl);
        body.put("srsProxyUrlSet", srsProxyUrl != null && !srsProxyUrl.isBlank());
        try {
            restTemplate.exchange(checkUrl, HttpMethod.GET, null, String.class);
            body.put("reachable", true);
            body.put("message", "Backend can reach SRS. WHEP/WHIP proxy should work.");
            return ResponseEntity.ok(body);
        } catch (HttpStatusCodeException e) {
            // 404/405 = SRS responded; connection works
            body.put("reachable", true);
            body.put("message", "Backend reached SRS (got " + e.getStatusCode() + "). WHEP/WHIP proxy should work.");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.warn("SRS health check failed url={} error={}", checkUrl, e.getMessage());
            body.put("reachable", false);
            body.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            body.put("message", "Backend cannot reach SRS. 502 on calls until fixed. Use srs-base-url=https://streaming.wakilfy.com (different VPS) or srs-proxy-url=http://127.0.0.1:1985 (same host only).");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }

    @PostMapping("/whip")
    public ResponseEntity<String> whip(
            @RequestParam(defaultValue = "live") String app,
            @RequestParam String stream,
            @RequestBody String sdp) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrlForProxy() + "/rtc/v1/whip/")
                .queryParam("app", app).queryParam("stream", stream).toUriString();
        return proxyPost("WHIP", url, stream, sdp);
    }

    @PostMapping("/whep")
    public ResponseEntity<String> whep(
            @RequestParam(defaultValue = "live") String app,
            @RequestParam String stream,
            @RequestBody String sdp) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrlForProxy() + "/rtc/v1/whep/")
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
            log.error("{} proxy failed stream={} url={} error={} (ensure backend can reach SRS; check firewall/DNS or set streaming.srs-proxy-url)", type, stream, url, e.getMessage(), e);
            String hint = " Ensure backend can reach SRS (e.g. " + srsBaseUrl + "). Check firewall/DNS or set streaming.srs-proxy-url if SRS is on same host.";
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(type + " proxy error: cannot reach SRS. " + e.getMessage() + hint);
        }
    }
}
