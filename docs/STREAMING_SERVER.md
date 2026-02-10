# Streaming Server (streaming.wakilfy.com)

**Status: TAYARI** – SRS inafanya kazi. Base URL: `https://streaming.wakilfy.com`

| Endpoint | Method | URL |
|----------|--------|-----|
| **Status** | GET | `/api/v1/versions` → 200 OK |
| **WHIP** (Broadcaster) | POST | `/rtc/v1/whip/?app=live&stream=JINA_LA_STREAM` |
| **WHEP** (Viewer) | POST | `/rtc/v1/whep/?app=live&stream=JINA_LA_STREAM` |

**Kumbuka:** SDP mbovu au tupu (k.m. Postman) → 502. Hii ni kawaida; SRS inakata invalid data. Valid WebRTC Offer (SDP) → 201 Created / 200 OK.

**Auth:** Open kwa sasa. Callback inaweza kuongezwa baadaye.

---

## WebRTC (Voice & Video Calls)

### TURN Credentials (required for calls)

| Field | Value |
|-------|-------|
| **Username** | `wakilfy` |
| **Password** | `Wakilfy@2026` |

### Servers & URLs

| Service | URL / Value |
|---------|-------------|
| **STUN Server** | `stun:streaming.wakilfy.com:3478` |
| **TURN Server** | `turn:streaming.wakilfy.com:3478` |
| **WHIP (Publish)** | `POST https://streaming.wakilfy.com/rtc/v1/whip/?app=live&stream=STREAM_KEY` |
| **WHEP (Play)** | `POST https://streaming.wakilfy.com/rtc/v1/whep/?app=live&stream=STREAM_KEY` |

## Livestream

| Service | URL |
|---------|-----|
| **RTMP (Upload)** | `rtmp://streaming.wakilfy.com/live/` |
| **HLS (Playback)** | `https://streaming.wakilfy.com/live/{stream_key}.m3u8` |

## Usage Notes

- **WebRTC calls (SRS):** SRS uses WHIP/WHEP HTTP API, NOT WebSocket. Caller publishes (WHIP) to `{roomId}_caller`, plays (WHEP) from `{roomId}_callee`. Callee does the opposite. Both must use HTTPS.
- **Livestream:** Ingest via RTMP; playback via HLS URL with your `stream_key`.

---

## Troubleshooting

### SRS inafanya kazi (livestream) lakini calls hazifanyi
- SRS inatumia **WHIP/WHEP** (HTTP POST), si WebSocket.
- Hakikisha SRS WebRTC iko enabled (`rtc_server.enabled on`).
- CORS: SRS lazima iruhusu requests kutoka `https://wakilfy.com`.

### Proxy (solutions ya CORS)
- Frontend inatumia **API proxy**: `POST /api/v1/streaming/whip` na `POST /api/v1/streaming/whep`
- Backend inaforward requests kwenye SRS. Hakuna CORS kwa sababu browser inazungumzia na API yake peke yake.

### 502 Bad Gateway
- **SDP invalid/tupu (Postman):** Kawaida. SRS inakata invalid data. Tumia valid WebRTC Offer (SDP) → 201/200.
- **Nginx body (production):** Upstream (SRS) haijajibu – angalia SRS iko running na Nginx ina proxy `/rtc/`:

**Suluhisho – config Nginx kwenye streaming server:**

```nginx
# Kwenye streaming.wakilfy.com - server block
location /rtc/ {
    proxy_pass http://127.0.0.1:1985;   # SRS HTTP API port (default 1985)
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

**Angalia:**
1. SRS iko running: `docker ps` au `systemctl status srs`
2. SRS HTTP API inasikiliza port 1985 (au port uliyotumia)
3. SRS config: `rtc_server.enabled on` na `rtc.enabled on` kwa vhost
4. Baada ya kubadilisha Nginx: `sudo nginx -t && sudo systemctl reload nginx`

### 500 / 502 proxy error (backend)
- **502 (badala ya nginx):** Backend haifiki streaming.wakilfy.com (firewall, DNS). Angalia backend logs.
- **SRS 404:** WHIP/WHEP path si sahihi. Hakikisha SRS ina WebRTC enabled.
