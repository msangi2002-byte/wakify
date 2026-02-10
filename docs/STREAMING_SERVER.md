# Streaming Server (streaming.wakilfy.com)

Credentials and endpoints for livestream, WebRTC voice/video calls, and HLS playback.

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
