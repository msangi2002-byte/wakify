# Live Streaming – Mawazo na Mfumo (Facebook/TikTok Style)

Hati hii inaelezea jinsi **Live** kwenye Wakify inavyofanya kazi: kutazama wanaolive, kuona live, kuomba kujoin live (guest), na kutuma gift.

---

## 1. Mawazo ya jumla (Product idea)

### Kama Facebook Live / TikTok Live

| Kipengele | Maelezo |
|-----------|---------|
| **Kuona wanaolive** | Mtu anapofungua app/section ya Live anaona **orodha ya watu wanaolive sasa** – picha, jina, idadi ya watu wanaotazama. |
| **Kutazama live** | Akibofya live moja, anaingia kwenye **ukurasa wa kuona video** – video ya host, viewer count, likes, chat/gifts. |
| **Kuomba kujoin live** | Viewer anaweza **kuomba kujiunga na live** (kama guest). Host anaona ombi, anaweza **kukubali** au **kukataa**. Akikubali, guest anaweza kuonekana kwenye live (voice/video ya pili). |
| **Kutuma gift** | Wakati wa kutazama live, viewer anaweza **kuchagua gift na kutuma** kwa host. Gift inaonekana kwenye live (animation) na thamani inaongezwa kwa host. |

---

## 2. Flow (mtiririko) wa mtumiaji

```
[Discovery]                    [Watch]                        [Join request]              [Gifts]
     |                             |                                  |                        |
  Open "Live" tab     →      Tap one live    →      Tap "Request to join"  →   Tap gift → Send
  See list of lives          Full screen             Host sees request            Host gets value
  (who is live now)          Watch video             Accept / Reject              (coins/wallet)
                             Like, comment           If accept → guest
                             Leave                   can go live (WHIP)
```

### Kwa kifupi

1. **Discovery (kuona wanaolive)**  
   - GET `/api/v1/live/active` – inarudisha orodha ya live zinazoendelea.  
   - UI: grid/list ya cards (thumbnail, host name, profile pic, viewer count, “LIVE” badge).

2. **Kutazama live (view)**  
   - GET `/api/v1/live/{liveId}` – taarifa za live (streamUrl, host, viewerCount, etc.).  
   - POST `/api/v1/live/{liveId}/join` – viewer “anaingia” (viewer count inaongezwa).  
   - Video: tumia **HLS** (`streamUrl` = `https://streaming.wakilfy.com/live/{streamKey}.m3u8`) au **WHEP** (WebRTC) kwa low latency.  
   - POST `/api/v1/live/{liveId}/leave` – alipoondoka.

3. **Kuomba kujoin live (request to join)**  
   - Viewer: POST `/api/v1/live/{liveId}/join-request` – “Nataka kujiunga na live kama guest.”  
   - Host: GET `/api/v1/live/{liveId}/join-requests` – anaona ombi zote (pending).  
   - Host: POST `/api/v1/live/join-requests/{requestId}/accept` au `.../reject`.  
   - Baada ya accept: backend inarudisha **guest stream key / room** (kwa WHIP) ili guest aweze ku-publish video/voice yake. (Implementation ya guest stream inaweza kuwa phase 2: kwanza tu request/accept/reject.)

4. **Kutuma gift**  
   - GET `/api/v1/gifts` – orodha ya virtual gifts.  
   - POST `/api/v1/gifts/send` – body: `receiverId` (host), `giftId`, `liveStreamId`, `quantity`, (optional) `message`.  
   - Host anapokea gift (value inaingia wallet); kwenye UI unaweza kuonyesha animation ya gift juu ya video.

---

## 3. API zilizopo vs zinazohitajika

### Zilizopo (backend iko tayari)

| Hatua | Method | Endpoint | Tumia |
|-------|--------|----------|--------|
| Config (STUN/TURN, RTC base) | GET | `/api/v1/live/config` | Frontend kujua WHEP/WHIP URL |
| Orodha ya live zinazoendelea | GET | `/api/v1/live/active?limit=20` | **Discovery page** – grid ya “who is live” |
| Taarifa za live moja | GET | `/api/v1/live/{liveId}` | **Watch page** – title, host, streamUrl, viewerCount, etc. |
| Anaanza live | POST | `/api/v1/live/start` | Host kuanza live (title, description) |
| Anamaliza live | POST | `/api/v1/live/{liveId}/end` | Host end live |
| Viewer ajiunga (view) | POST | `/api/v1/live/{liveId}/join` | Increment viewer count |
| Viewer aondoka | POST | `/api/v1/live/{liveId}/leave` | Decrement viewer count |
| Like | POST | `/api/v1/live/{liveId}/like` | Like live |
| Orodha ya gifts | GET | `/api/v1/gifts` | **Watch page** – pick gift to send |
| Kutuma gift | POST | `/api/v1/gifts/send` | Body: `receiverId`, `giftId`, `liveStreamId`, `quantity`, `message` |
| Wallet | GET | `/api/v1/wallet` | Coins balance (kwa kununua coins / kutuma gifts) |

### Zinazohitajika (zimeongezwa)

| Hatua | Method | Endpoint | Tumia |
|-------|--------|----------|--------|
| Viewer omba kujoin | POST | `/api/v1/live/{liveId}/join-request` | “Nataka kujiunga kama guest” |
| Host aona ombi | GET | `/api/v1/live/{liveId}/join-requests` | Orodha ya pending (na accepted/rejected ikiwa unataka) |
| Host kubali | POST | `/api/v1/live/join-requests/{requestId}/accept` | Guest anaweza ku-publish (phase 2) |
| Host kataa | POST | `/api/v1/live/join-requests/{requestId}/reject` | Omb la guest linakataliwa |

---

## 4. Video: HLS vs WHEP

- **HLS** (`streamUrl` kutoka `GET /api/v1/live/{liveId}`):  
  `https://streaming.wakilfy.com/live/{streamKey}.m3u8` – rahisi, inafanya kazi na video player (hls.js, etc.). Latency kubwa kidogo (10–30s).

- **WHEP** (WebRTC):  
  `POST https://streaming.wakilfy.com/rtc/v1/whep/?app=live&stream={streamKey}` – latency chini. Base URL unapata kwenye `GET /api/v1/live/config` → `rtcApiBaseUrl`.  
  Kwa **watch page** unaweza kuchagua: HLS kwa urahisi, WHEP kwa low latency.

Host anapublish kwa **WHIP** (au RTMP) kwenye `streamKey` yake; viewers wanacheza kwa HLS au WHEP kwa `streamKey` ile ile.

---

## 5. Gifts – jinsi inavyofanya kazi

- **Coins:** Mtumiaji ananunua coins (GET `/api/v1/coins/packages`, POST `/api/v1/coins/purchase`), balance iko GET `/api/v1/wallet`.
- **Virtual gifts:** GET `/api/v1/gifts` – kila gift ina `coin_value` (na optional `price` TZS). Kutuma gift kunapunguza coins za sender, kuingiza value kwa receiver (host).
- **Wakati wa live:** POST `/api/v1/gifts/send` na `liveStreamId` – gift inaunganishwa na live; host anaweza kuona “gifts received during this stream” na thamani ya live inaongezwa (`totalGiftsValue` kwenye live).

Kwenye **watch page** unaweza kuweka:
- Kitufe “Gift” kinacho fungua drawer/modal ya gifts.
- Baada ya kuchagua gift na quantity → POST `/api/v1/gifts/send` (receiverId = host id, liveStreamId = live id).
- Optional: real-time updates (WebSocket/polling) kuonyesha “X sent gift” juu ya video.

---

## 6. Request to join (guest) – muundo wa data

- **LiveStreamJoinRequest**:  
  - `id`, `liveStreamId`, `requesterId` (viewer), `status` (PENDING, ACCEPTED, REJECTED), `hostRespondedAt`, `createdAt`.  
- Viewer anatumia POST `/api/v1/live/{liveId}/join-request`.  
- Host anatumia GET `/api/v1/live/{liveId}/join-requests`, halafu accept/reject.  
- Baada ya **accept** (phase 2): unaweza kutoa “guest stream” (mf. `roomId_guest` au stream key ya pili) ili guest atumie WHIP ku-publish; kwa sasa inaweza kuwa tu accept/reject na notification kwa guest.

---

## 7. UI suggestions (kwa frontend)

1. **Live tab / Discovery page**  
   - Section “Live now” na grid ya cards.  
   - Kila card: thumbnail (au profile pic ya host), jina la host, viewer count, badge “LIVE”.  
   - Tap → navigate to watch page (`/live/{liveId}`).

2. **Watch page**  
   - Video (HLS au WHEP) full-width.  
   - Overlay: host name, viewer count, like button.  
   - Bottom/side: “Request to join”, “Gift”, “Share”.  
   - Join: POST join, Leave: POST leave (e.g. on page leave).

3. **Gift drawer**  
   - List ya gifts (picha, jina, coin value).  
   - Chagua quantity → Send → POST `/api/v1/gifts/send`.

4. **Host:** Orodha ya join requests (sidebar au modal), Accept / Reject kwa kila ombi.

---

## 8. Muhtasari

| Kipengele | Status | API / Note |
|-----------|--------|------------|
| Kuona wanaolive (discovery) | ✅ | GET `/api/v1/live/active` |
| Kutazama live (view) | ✅ | GET live by id, POST join/leave, streamUrl (HLS/WHEP) |
| Kuomba kujoin (guest) | ✅ | Join-request API (see above) |
| Kutuma gift wakati wa live | ✅ | POST `/api/v1/gifts/send` + `liveStreamId` |
| Host accept/reject guest | ✅ | Accept/Reject endpoints |

Frontend inahitaji: **Discovery page** (grid ya live), **Watch page** (video + join + gift + join-request), na **Host UI** ya join requests (accept/reject). Backend ya join-request iko kwenye repo (entity, repository, service, controller).
