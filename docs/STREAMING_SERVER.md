# Streaming Server (streaming.wakilfy.com)

**Status: Tested & Verified** – 502, CORS, UDP Ports zimetatuliwa. Base URL: `https://streaming.wakilfy.com`

| Endpoint | Method | URL |
|----------|--------|-----|
| **Status** | GET | `/api/v1/versions` → 200 OK |
| **WHIP** (Publisher) | POST | `/rtc/v1/whip/?app=live&stream=[UNIQUE_STREAM_ID]` → 201 Created |
| **WHEP** (Player) | POST | `/rtc/v1/whep/?app=live&stream=[UNIQUE_STREAM_ID]` |

**Ports:** UDP 8000 (Media), TCP 1985 (API). Nginx inaunganisha.

**Flow muhimu (kuepuka 502):**
- **Caller:** Lazima WHIP ipate 201 Created (stream imeanza) kabla ya mtu mwingine ku-WHEP.
- **Callee:** Usi-WHEP mpaka Caller apate 201. Ukicheza stream isiyopo → 404/502.

**CORS:** `Access-Control-Allow-Origin: *` – domain yoyote inaruhusiwa.

**Auth:** Open kwa sasa.

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

## 502 bado? – Check health (hatua ya kwanza)

1. **Open kwenye browser (bila login):**  
   **https://wakilfy.com/api/v1/streaming/health**

2. **Angalia response (JSON):**
   - **`reachable: true`** → Backend inafiki SRS; 502 inaweza kutokana na kitu kingine (k.m. stream haijaanza).
   - **`reachable: false`** → Backend **haifiki** SRS. Angalia `proxyTargetUrl` na `error`:
     - `proxyTargetUrl` = `http://127.0.0.1:1985/...` na SRS iko kwenye server **tofauti** → **tatizo ni config.** On the server where the Java app runs: **remove** `streaming.srs-proxy-url` (or do not set it to 127.0.0.1). Use only `streaming.srs-base-url=https://streaming.wakilfy.com`, then restart the app.
     - `error` = Connection refused / Unknown host / timeout → on that server fix firewall/DNS so it can reach `https://streaming.wakilfy.com` (e.g. `curl -I https://streaming.wakilfy.com`).

3. **Kama hauna server access:** Badilisha **frontend** kutumia SRS moja kwa moja: `POST https://streaming.wakilfy.com/rtc/v1/whep/?app=live&stream=...` (na whip). URL hii unapata kwenye `GET /api/v1/live/config` → `rtcApiBaseUrl`. Hivyo 502 ya proxy haihusiki.

---

## URGENT FIX: 502 Proxy Error (copy kwa Backend Dev)

**Ujumbe unaoweza kutuma kwa Backend Developer:**

> **URGENT FIX: 502 Proxy Error on Main API**
>
> The 502 is from the Main Backend (wakilfy.com / nginx), NOT the SRS Server. The app hits `POST https://wakilfy.com/api/v1/streaming/whep`; the Backend tries to proxy to SRS and fails.
>
> **Fix:** Update Backend config (`.env` or `application.properties`). Do NOT point the proxy to `http://127.0.0.1:1985` — SRS is on a different VPS.
>
> **Set SRS Base URL to one of:**
> - `https://streaming.wakilfy.com`
> - `http://107.152.35.163:1985`
>
> Property name (Java): **`streaming.srs-base-url`**  
> Remove or leave unset: **`streaming.srs-proxy-url`** (only use 127.0.0.1 if SRS runs on the same machine as the app).
>
> After changing, restart the Backend. Then the 502 on the call screen should stop.

**Kwa nini:** Backend imesetiwa kama “middleman” — app haiongei na SRS moja kwa moja, inapita kwenye Backend. Backend lazima ielekeze kwenye anwani halisi ya SRS (domain au IP 107.152.35.163), si localhost.

---

## Technical fix for 502 on Video/Voice calls (details)

- App inapiga Main API: `https://wakilfy.com/api/v1/streaming/whep` (proxy). Proxy inashindwa (502) kwa sababu Backend haifiki SRS.
- **Lazima config ielekeze SRS kwenye server halisi:**
  - **SRS kwenye VPS tofauti:** `streaming.srs-base-url` = **`https://streaming.wakilfy.com`** au **`http://107.152.35.163:1985`**. **Usiweke** `streaming.srs-proxy-url` (au usiweke 127.0.0.1). Hakikisha server inayokwenda Java inaweza kufikia URL hiyo (`curl`).
fix call 008  - **SRS kwenye server moja na App:** `streaming.srs-proxy-url=http://127.0.0.1:1985`.
- **Best practice:** Frontend iite SRS moja kwa moja: `RTC_API_URL = https://streaming.wakilfy.com/rtc/v1` (WHIP/WHEP). Backend inatoa hii kwenye `GET /api/v1/live/config` → `rtcApiBaseUrl`. Hivyo traffic haipiti proxy; hakuna 502.

---

## Troubleshooting

### SRS inafanya kazi (livestream) lakini calls hazifanyi
- SRS inatumia **WHIP/WHEP** (HTTP POST), si WebSocket.
- Hakikisha SRS WebRTC iko enabled (`rtc_server.enabled on`).
- CORS: SRS lazima iruhusu requests kutoka `https://wakilfy.com`.

### Proxy vs Direct (kwa Video/Voice calls)
- **Njia ya sasa (proxy):** Frontend inapiga `POST https://wakilfy.com/api/v1/streaming/whep` (na whip). Backend (wakilfy.com) inaenda ku-forward kwa SRS. **502 inatokea wakati Backend haifiki SRS** (k.m. Backend iko kwenye VPS tofauti na imesetiwa kutafuta SRS kwenye `127.0.0.1`).
- **Config sahihi:** SRS iko kwenye **server tofauti** (streaming.wakilfy.com) → **usiwahi** kuweka `streaming.srs-proxy-url=http://127.0.0.1:1985`. Tumia `streaming.srs-base-url=https://streaming.wakilfy.com` na hakikisha server inayokwenda Java inaweza kufikia URL hiyo (curl/firewall).
- **Same VPS tu:** Ikiwa app na SRS ziko kwenye server moja, basi weka `streaming.srs-proxy-url=http://127.0.0.1:1985`.
- **Best practice (direct):** Frontend inaweza kuwasiliana na SRS moja kwa moja: `POST https://streaming.wakilfy.com/rtc/v1/whep/?app=live&stream=...` (na whip). CORS kwenye SRS inaruhusu (`*`). Backend inatoa URL hii kwenye `GET /api/v1/live/config` → `rtcApiBaseUrl`. Hivyo hakuna proxy, hakuna 502 kutoka backend.

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

### 500 / 502 proxy error (backend) – "Proxy via API. Ensure you are logged in and backend can reach SRS"
- **502 on POST /api/v1/streaming/whep (or whip):** Backend (wakilfy.com) ni "middleman" – inapokea request kutoka browser, halafu inajaribu ku-forward kwa SRS. 502 = Backend **haipati** SRS (connection refused, timeout, SSL, DNS). **Kosa la kawaida:** Backend na SRS ziko kwenye **VPS tofauti** lakini config inasema `127.0.0.1:1985` – Backend inagonga "ukuta" kwa kuita localhost ya server yake, si streaming.wakilfy.com.
- **Angalia:** Backend logs (exception: Connection refused, UnknownHost, timeout, SSL).
- **Suluhisho 1 – VPS tofauti (kawaida):** **Usiweke** `streaming.srs-proxy-url`. Acha `streaming.srs-base-url=https://streaming.wakilfy.com`. Kutoka **server inayokwenda Java app** jaribu: `curl -I https://streaming.wakilfy.com`. Ikiwa haifiki, rekebisha firewall/DNS/SSL.
- **Suluhisho 2 – same VPS:** Ikiwa app na SRS ziko kwenye server moja, weka `streaming.srs-proxy-url=http://127.0.0.1:1985`.
- **Suluhisho 3 – Direct (best practice):** Epuka proxy kabisa. Frontend iite SRS moja kwa moja: `POST {rtcApiBaseUrl}/whep/?app=live&stream=...`. `GET /api/v1/live/config` inarudisha `rtcApiBaseUrl` (mf. `https://streaming.wakilfy.com/rtc/v1`). CORS kwenye SRS inaruhusu; auth kwa sasa open.
- **SRS 404:** Stream haijaanza. Callee asiwhep mpaka Caller apate 201 kwenye WHIP.
