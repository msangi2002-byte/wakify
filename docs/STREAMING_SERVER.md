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
| **WebRTC Signal (Signaling)** | `wss://streaming.wakilfy.com/rtc/v1/sig` |

## Livestream

| Service | URL |
|---------|-----|
| **RTMP (Upload)** | `rtmp://streaming.wakilfy.com/live/` |
| **HLS (Playback)** | `https://streaming.wakilfy.com/live/{stream_key}.m3u8` |

## Usage Notes

- **WebRTC calls:** Connect to `wss://streaming.wakilfy.com/rtc/v1/sig` for signaling. Backend creates a Call with `roomId` via `POST /api/v1/calls/initiate`; frontend uses `roomId` to join the room via the signaling server.
- **Livestream:** Ingest via RTMP; playback via HLS URL with your `stream_key`.
