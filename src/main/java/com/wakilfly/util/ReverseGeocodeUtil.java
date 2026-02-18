package com.wakilfly.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reverse geocoding: latitude/longitude → country (and optionally region).
 * Uses OpenStreetMap Nominatim (free, no API key). Caches results to reduce API calls.
 * When user has lat/long from GPS but no country (e.g. registration), we derive country for map stats.
 */
@Slf4j
public final class ReverseGeocodeUtil {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse?lat=%s&lon=%s&format=json&zoom=3&addressdetails=1";
    private static final String USER_AGENT = "Wakilfly-Admin/1.0 (map-stats)";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final RestTemplate REST = new RestTemplate();
    private static final Map<String, CachedResult> CACHE = new ConcurrentHashMap<>();
    private static volatile long lastRequestTime = 0;
    private static final long MIN_INTERVAL_MS = 1100; // Nominatim: 1 req/sec

    public static class Result {
        public final String country;
        public final String region;

        public Result(String country, String region) {
            this.country = country != null ? country.trim() : null;
            this.region = region != null ? region.trim() : null;
        }
    }

    private static class CachedResult {
        final String country;
        final String region;

        CachedResult(String country, String region) {
            this.country = country;
            this.region = region;
        }
    }

    /**
     * Get country (and optionally region) from coordinates.
     * Returns null if geocoding fails. Uses cache for repeated lookups.
     */
    public static Result geocode(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) return null;
        String key = cacheKey(latitude, longitude);
        CachedResult cached = CACHE.get(key);
        if (cached != null) {
            return new Result(cached.country, cached.region);
        }
        Result result = fetchFromNominatim(latitude, longitude);
        if (result != null && result.country != null) {
            CACHE.put(key, new CachedResult(result.country, result.region));
        }
        return result;
    }

    private static String cacheKey(double lat, double lng) {
        return String.format("%.2f,%.2f", lat, lng);
    }

    /**
     * Clear the in-memory geocode cache (e.g. for admin maintenance).
     * @return number of entries removed
     */
    public static int clearCache() {
        int size = CACHE.size();
        CACHE.clear();
        log.info("Geocode cache cleared ({} entries)", size);
        return size;
    }

    /** Current number of cached entries (for admin/system info). */
    public static int cacheSize() {
        return CACHE.size();
    }

    private static Result fetchFromNominatim(double lat, double lng) {
        synchronized (ReverseGeocodeUtil.class) {
            long now = System.currentTimeMillis();
            long wait = MIN_INTERVAL_MS - (now - lastRequestTime);
            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            lastRequestTime = System.currentTimeMillis();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            String url = String.format(NOMINATIM_URL, lat, lng);
            var resp = REST.exchange(url, HttpMethod.GET, entity, String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode root = MAPPER.readTree(resp.getBody());
                JsonNode addr = root.path("address");
                String country = addr.has("country") ? addr.get("country").asText().trim() : null;
                String region = null;
                for (String k : new String[]{"state", "region", "county", "state_district"}) {
                    if (addr.has(k) && !addr.get(k).asText().isBlank()) {
                        region = addr.get(k).asText();
                        break;
                    }
                }
                return new Result(country, region);
            }
        } catch (Exception e) {
            log.debug("Reverse geocode failed for {}, {}: {}", lat, lng, e.getMessage());
        }
        return null;
    }

    private ReverseGeocodeUtil() {}
}
