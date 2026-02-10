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
| **HLS (Playback)** | `https://streaming.wakilfy.com/live/{STREAM_KEY}.m3u8` |

**Confirmed (SRS reconfigured):** Server inatumia **direct .m3u8** (si `/index.m3u8`). Backend/App lazima itumie format hapo juu kwa HLS playback.

### SRS Server config (Docker) – HLS enabled

- **HLS enabled:** `SRS_VHOST_HLS_ENABLED=on` – WebRTC (WHIP) streams zinabadilishwa kuwa HLS (.m3u8) automatically.
- **RTC → RTMP:** `SRS_VHOST_RTC_RTC_TO_RTMP=on` – inasaidia conversion kwa HLS.
- **Fragment:** HLS fragment = 2 sekunde (playback inaanza haraka).
- **Playback URL (confirmed):** `https://streaming.wakilfy.com/live/{STREAM_KEY}.m3u8`

**Note:** Streaming host lazima awe **hewani (publishing)** – RTMP au WHIP – ndipo faili .m3u8 itengenezwe. Bila publish, `.m3u8` haipo → 404.

---

## 404 on HLS (.m3u8) – Blank video / “video haionekani”

Ikiwa watumiaji wanaona **blank** na F12 inaonyesha **404** kwenye:
`https://streaming.wakilfy.com/live/{stream_key}.m3u8`

**Sababu 1 – Video haijaanza kwenye server (kawaida)**  
"Go live" kwenye app inaunda tu **rekodi** kwenye DB na inakupa **RTMP URL**. Video inaonekana **tu** baada ya host **ku-push stream** kwenye streaming server:

1. Host afungue **OBS** (au app nyingine ya RTMP).
2. **Settings → Stream**:  
   - Service: Custom  
   - Server: `rtmp://streaming.wakilfy.com/live/`  
   - Stream key: **stream key** (kama `c0e8302975954c47bbc8b1d325e1a45d` – unaonyeshwa kwenye ukurasa wa live kwa host).
3. **Start Streaming**.  
Mpaka hapo stream haijafika SRS → hakuna `.m3u8` → 404. Baada ya OBS kuanza, HLS inaweza kuchukuwa sekunde chache kuanza.

**Sababu 2 – Path ya HLS (kwenye server hii: direct .m3u8)**  
Kwenye streaming.wakilfy.com format **ilithibitishwa**: `.../live/{STREAM_KEY}.m3u8` (si `/index.m3u8`). Backend inatumia `streaming.hls-path-suffix=.m3u8` – sahihi.

**Mwenye streaming server (streaming.wakilfy.com) aangalie:**
- SRS ina **HTTP server** kwa HLS na path `/live/`?
- Nginx ina **proxy** kwa `/live/` kwenda SRS?
- Host lazima awe **publishing** (OBS/RTMP au WHIP); kisha `curl -I "https://streaming.wakilfy.com/live/STREAM_KEY.m3u8"` ndani ya server inaweza kuthibitisha .m3u8 iko.

---

## Usage Notes

- **WebRTC calls (SRS):** SRS uses WHIP/WHEP HTTP API, NOT WebSocket. Caller publishes (WHIP) to `{roomId}_caller`, plays (WHEP) from `{roomId}_callee`. Callee does the opposite. Both must use HTTPS.
- **Livestream:** Ingest via RTMP; playback via HLS URL with your `stream_key`.

---

## UI / Frontend iko sawa – 502 ni Backend tu

- **502 inatoka wakilfy.com (Backend proxy), si kwenye UI.** Frontend inatumia `/api/v1/streaming/whep` na `/whip`; Backend inapaswa ku-forward kwa SRS. Inapo fail, response ni 502. Kurekebisha: **config kwenye server inayokwenda Backend** (si code ya frontend).

---

## ✅ Imethibitishwa: Video inafika SRS, Playback (WHEP) ndiyo inashindwa

**Uchunguzi (SRS Console):** Streams zinazoonekana kwenye SRS (mf. `call_c1300203_caller` ~1.42Mbps, `call_c1300203_callee` ~55Kbps) zina maana **WHIP (ingest) inafanya kazi** – video inafika kwenye server. **Tatizo:** App inashindwa **kucheza** (Play) video kwa sababu inatumia Proxy URL (`https://wakilfy.com/api/v1/streaming/whep`) inayorudisha 502. Yaani tatizo si SRS wala ingest; ni **Playback URL** kwenye App.

**Suluhisho (Frontend):** Badilisha **Playback URL (WHEP)** kwenye App ielekee **moja kwa moja** kwenye SRS; **usitumie** Main API proxy kwa playback.

| Nini | Usitumie (proxy – 502) | Tumia (direct) |
|------|------------------------|-----------------|
| **WHEP (Play)** | `https://wakilfy.com/api/v1/streaming/whep?app=live&stream=STREAM_ID` | `https://streaming.wakilfy.com/rtc/v1/whep/?app=live&stream=STREAM_ID` |
| **WHIP (Publish)** | `https://wakilfy.com/api/v1/streaming/whip?app=live&stream=STREAM_ID` | `https://streaming.wakilfy.com/rtc/v1/whip/?app=live&stream=STREAM_ID` |

Base URL (e.g. `https://streaming.wakilfy.com/rtc/v1`) unapata kwenye **`GET /api/v1/live/config`** → `rtcApiBaseUrl`. Ukishabadilisha Frontend kutumia `rtcApiBaseUrl` kwa WHEP na WHIP, "Waiting for other person" inaisha na playback inafanya kazi.

**Manual test:** WHEP Player → URL: `https://streaming.wakilfy.com/rtc/v1/whep/?app=live&stream=call_c1300203_caller` → Play; unapaswa kuona video ya mpigaji simu.

**Kuhusu "Secure Connection Failed (PR_END_OF_FILE_ERROR)" kwenye Preview:** Console inajaribu `https://streaming.wakilfy.com:8080`; port 8080 ni HTTP (plain), si HTTPS. Si kosa la server – usitumie kitufe cha Preview hapo.

---

## Ujumbe kwa Frontend/Backend Dev (copy-paste)

> **Video Calls are Reaching SRS, but Playback is Failing**
>
> SRS Console shows active streams (e.g. call_*_caller at ~1.42Mbps, call_*_callee). So **ingest (WHIP) is OK** – video reaches the server.
>
> **Problem:** The App fails to **play** the video because it still uses the Proxy URL `https://wakilfy.com/api/v1/streaming/whep`, which returns **502**.
>
> **Fix:** Point the **Player (WHEP)** and, if used, **Publisher (WHIP)** in the App **directly to SRS**, not via the Main API:
> - **WHEP (playback):** `https://streaming.wakilfy.com/rtc/v1/whep/?app=live&stream=STREAM_ID`
> - **WHIP (publish):** `https://streaming.wakilfy.com/rtc/v1/whip/?app=live&stream=STREAM_ID`
>
> The base URL is already returned by **`GET /api/v1/live/config`** as **`rtcApiBaseUrl`**. Use it to build WHEP/WHIP URLs. Do not use `wakilfy.com/api/v1/streaming/...` for playback – connect the player directly to the Streaming Server. After this change, "Waiting for other person" should stop and playback will work.

---

## 502 kutoka streaming.wakilfy.com (Server: nginx/1.24.0)

Ikiwa 502 inatoka **streaming.wakilfy.com** (si wakilfy.com), request inafika **streaming server** lakini **nginx** kwenye hiyo server inarudisha 502. Maana: **upstream (SRS) haijajibu** au haipo – tatizo ni kwenye **streaming VPS**, si frontend wala main backend.

**Lazima mwenye streaming server (streaming.wakilfy.com) afanye:**

1. **Hakikisha SRS inaendesha:** `docker ps` au `systemctl status srs` (au jinsi SRS ilivyo install).
2. **SRS HTTP API inasikiliza port 1985:** SRS config lazima iwe na API on 1985; kawaida `/rtc/v1/whep/` na `/rtc/v1/whip/` zinafikia SRS.
3. **Nginx ina proxy `/rtc/` kwa SRS:** Kwenye streaming server, config ya nginx lazima iwe na kitu kama:
   ```nginx
   location /rtc/ {
       proxy_pass http://127.0.0.1:1985;
       proxy_http_version 1.1;
       proxy_set_header Host $host;
       proxy_set_header X-Real-IP $remote_addr;
       proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
       proxy_set_header X-Forwarded-Proto $scheme;
   }
   ```
4. **Jaribu kutoka ndani ya server:** `curl -X POST "http://127.0.0.1:1985/rtc/v1/whep/?app=live&stream=test" -H "Content-Type: application/sdp" -d "v=0"` – angalia kama SRS inajibu (404/405 inaeleweka; connection refused = SRS haijaisha au port wrong).

**Ujumbe kwa mwenye streaming server (copy-paste):**

> **502 on POST https://streaming.wakilfy.com/rtc/v1/whep/ (and /whip/)**  
> The browser is now calling your streaming server directly. The 502 is returned by **nginx on streaming.wakilfy.com** (Server: nginx/1.24.0), so the problem is on the streaming VPS: nginx is proxying to SRS but the upstream is not responding.
>
> Please:
> 1. Ensure SRS is running (e.g. `docker ps` or `systemctl status srs`).
> 2. Ensure SRS HTTP API is listening on port 1985 (or the port nginx proxies to).
> 3. Ensure nginx has `location /rtc/ { proxy_pass http://127.0.0.1:1985; ... }` (and reload nginx).
> 4. Test from the streaming server: `curl -v http://127.0.0.1:1985/rtc/v1/` – you should get a response (e.g. 404), not "connection refused".

### Streams zinaonekana SRS Console lakini WHEP bado 502

Ikiwa kwenye **SRS Console** (streaming.wakilfy.com/console) unaona streams kama `call_XXXX_caller` na `call_XXXX_callee` (Video H264, Audio AAC, Clients 1) – **WHIP inafanya kazi**; data inafika SRS. Inbound/Outbound 0.00Kbps inaweza kuwa mwanzo wa stream au console inaonyesha baadaye.

Lakini **WHEP** bado inarudisha **502** (POST …/whep/?app=live&stream=call_XXX_callee). Hiyo ina maana tatizo si WHIP wala frontend; ni **kwenye streaming server** – either nginx haipiti vizuri request ya WHEP kwa SRS, au SRS inarudisha error na nginx inaibadilisha kuwa 502.

**Mwenye streaming server anapaswa:**

1. **Nginx – kuwa na proxy sahihi kwa POST na body (SDP):**  
   Ndani ya `location /rtc/` ongeza (ikiwa haipo):
   ```nginx
   client_max_body_size 64k;
   proxy_read_timeout 60s;
   proxy_send_timeout 60s;
   ```
   Kisha `sudo nginx -t && sudo systemctl reload nginx`.

2. **Logs – kuona kosa halisi:**  
   - Nginx error log: `sudo tail -f /var/log/nginx/error.log`  
   - SRS log (au docker logs) wakati unafanya WHEP request.  
   Angalia kama nginx inasema "upstream prematurely closed" au SRS inarudisha 4xx/5xx.

3. **Jaribu WHEP kutoka ndani ya server:**  
   `curl -v -X POST "http://127.0.0.1:1985/rtc/v1/whep/?app=live&stream=call_TEST_caller" -H "Content-Type: application/sdp" -d "v=0"`  
   Angalia jibu la SRS (404 inaeleweka; 502/connection refused inaonyesha SRS au proxy shida).

**Ujumbe mfupi kwa mwenye streaming server:**

> Streams show in SRS Console (WHIP works), but **WHEP** still returns **502**. So the issue is nginx or SRS on the streaming server. Please: (1) Add `client_max_body_size 64k;` and `proxy_read_timeout 60s;` in nginx `location /rtc/`, then reload nginx. (2) Check nginx error log and SRS log when a WHEP POST is made. (3) Test WHEP from inside the server: `curl -X POST "http://127.0.0.1:1985/rtc/v1/whep/?app=live&stream=test" -H "Content-Type: application/sdp" -d "v=0"` and see the response.

### SRS: "remote sdp check failed : now only support BUNDLE" (WHEP 502)

Ikiwa SRS logs zinaonyesha **offer=105B** au **offer=265B** kwa WHEP (play) na **remote sdp check failed : now only support BUNDLE** – tatizo ni **frontend**: SDP ya playback ni ndogo/incomplete (hakuna media sections sahihi). WHIP inafanikiwa (offer=1006B, 4319B) kwa sababu publish PC ina tracks; play PC inaundwa bila transceivers, hivyo `createOffer()` inatoa SDP isiyokidhi SRS.

**Fix kwenye frontend (App):** Kabla ya `createOffer()` kwa **playback** PeerConnection (WHEP), lazima uongeze recvonly transceivers:

```javascript
// Playback PC – kabla ya createOffer()
playPc.addTransceiver('audio', { direction: 'recvonly' });
playPc.addTransceiver('video', { direction: 'recvonly' });
const offer = await playPc.createOffer();
```

Bila hivi SDP inayotumwa kwa WHEP ni &lt;300B na SRS inakataa. Baada ya kuongeza, SDP inakuwa full WebRTC SDP (&gt;1KB) yenye `m=audio`, `m=video`, na BUNDLE.

---

### "Empty reply from server" (curl 52) – SRS inasikiliza lakini haijui kurudisha response

Ikiwa kwenye server umepata **Connected to 127.0.0.1:1985** lakini **Empty reply from server** / **curl: (52)** – maana ni: kitu kinasikiliza port 1985 (kawaida SRS), lakini **hakijarudishi HTTP response** (inafungia connection bila kujibu). Nginx inapoproxia WHEP kwa 1985, inapokea "empty" → inarudisha **502 Bad Gateway**. Tatizo ni **SRS (au process kwenye 1985)**, si nginx wala frontend.

**Lazima mwenye streaming server:**

1. **Angalia SRS logs** wakati unafanya hiyo curl (au wakati app inapiga WHEP):  
   `docker logs -f <srs_container>` au `journalctl -u srs -f` au log file ya SRS. Angalia crash, assertion, au error inayohusiana na WHEP/WebRTC.
2. **Hakikisha SRS ina WebRTC (WHEP) enabled:** Config ya SRS lazima iwe na `rtc_server { enabled on; }` na vhost na `rtc { enabled on; }`. SRS version lazima isupport WHEP (e.g. 5.0+).
3. **Jaribu WHIP kwenye 1985** (kwa kulinganisha):  
   `curl -v -X POST "http://127.0.0.1:1985/rtc/v1/whip/?app=live&stream=test" -H "Content-Type: application/sdp" -d "v=0"`  
   Ikiwa WHIP pia inatoa empty reply, tatizo ni jumla (SRS HTTP API / WebRTC config). Ikiwa WHIP inajibu (e.g. 201/404) na WHEP tu inatoa empty, tatizo ni maalum kwa WHEP handler.
4. **Restart SRS** baada ya kubadilisha config, kisha jaribu tena curl ya WHEP.

**Ujumbe kwa mwenye streaming server:**

> When testing WHEP on the server we get: **Connected to 127.0.0.1:1985** but **Empty reply from server (curl 52)**. So the process on 1985 accepts the connection but does not send any HTTP response – that’s why nginx returns 502. Please: (1) Check SRS logs when the curl or app does the WHEP request (look for crash/error). (2) Ensure SRS has WebRTC/WHEP enabled in config (`rtc_server { enabled on; }`, vhost `rtc { enabled on; }`). (3) Compare: try the same with WHIP (`/rtc/v1/whip/`). If WHIP responds but WHEP gives empty reply, the issue is in the WHEP handler or config. (4) Restart SRS after config changes and test again.

---

## 502 bado? – Check health (hatua ya kwanza)

1. **Open kwenye browser (bila login):**  
   **https://wakilfy.com/api/v1/streaming/health**

2. **Angalia response (JSON):**
   - **`reachable: true`** → Backend inafiki SRS; 502 inaweza kutokana na kitu kingine (k.m. stream haijaanza).
   - **`reachable: false`** → Backend **haifiki** SRS. Angalia `proxyTargetUrl` na `error`:
     - `proxyTargetUrl` = `http://127.0.0.1:1985/...` na SRS iko kwenye server **tofauti** → **tatizo ni config.** On the server where the Java app runs: **remove** `streaming.srs-proxy-url` (or do not set it to 127.0.0.1). Use only `streaming.srs-base-url=https://streaming.wakilfy.com`, then restart the app.
     - `error` = Connection refused / Unknown host / timeout → on that server fix firewall/DNS so it can reach `https://streaming.wakilfy.com` (e.g. `curl -I https://streaming.wakilfy.com`).

3. **Workaround (bila kubadilisha server):** Badilisha **frontend** kutumia SRS moja kwa moja badala ya proxy:
   - **Badala ya:** `POST https://wakilfy.com/api/v1/streaming/whep?app=live&stream=...` (na `/whip`).
   - **Tumia:** `POST https://streaming.wakilfy.com/rtc/v1/whep/?app=live&stream=...` (na `.../whip/`).
   - Base URL unapata kwenye **`GET /api/v1/live/config`** → `rtcApiBaseUrl` (tayari backend inarudisha hii). Frontend ikiweka base ya WHEP/WHIP kuwa `rtcApiBaseUrl`, 502 inaisha kwa sababu browser inapiga SRS moja kwa moja (CORS imeruhusu).

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
- **SRS kwenye server moja na App:** `streaming.srs-proxy-url=http://127.0.0.1:1985`.
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
