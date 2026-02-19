# Become a Business – UI/UX na Orodha ya Agents (Settings Page)

Hati hii inaelezea **UI nzuri na UX** kwa kipande "Become a business" kwenye **Settings** (`/app/settings`), pamoja na **orodha ya agents** (kwa popularity, rating, na karibu), **ramani (map)** na **popup ya kukadiria agent** baada ya activation.

---

## 1. Flow Kamili (User)

1. User anaenda **Settings** → **Become a business**.
2. Akipenda kuwa business, anabonyeza **"Request to open a business"** (au kitufe sawa).
3. **Modal / full-screen** inafunguka na:
   - **Chaguo la kuonyesha agents:** **Map** (default) au **List**.
   - **Map view:** Ramani inaonyesha **agents wenye location** na **online** (walio karibu na user ikiwa app ina location). User anaweza kubonyeza **marker** ya agent kuona **details** (name, rating, businesses activated, code) na kuchagua **"Request with this agent"**.
   - **List view:** Orodha ya agents iliyopangwa kwa:
     - **Popularity** – wengi businesses waliowasajili (default).
     - **Rating** – rating ya juu kutoka kwa business owners waliowasajili.
     - **Nearby** – walio karibu na user (lat/lng kutoka profile au browser).
4. User **anachagua agent** (kutoka map au list) → anajaza **fomu** (business name, phone, category, region, n.k.) na **agentCode** inatumwa kwa backend (request inaenda **direct kwa agent** yule).
5. Baada ya **submit** → malipo (USSD / demo) → baada ya **success**, user ana **role BUSINESS**.
6. **Popup ya rating:** Baada ya activation, frontend inaonyesha **popup** ya kumpa **rating (1–5)** na optional comment kwa **agent** aliyemwasajilia business. Akiisha submit, request inaenda **POST /api/v1/users/me/rate-agent**.

---

## 2. Backend APIs (tayari)

### 2.1 Orodha ya agents (kwa Become a business)

- **GET** `/api/v1/agent/for-business-request`
  - **Auth:** Bearer token (authenticated).
  - **Query:**  
    - `sort` = `popularity` | `rating` | `nearby` (default: `popularity`).  
    - `lat`, `lng` = required ikiwa `sort=nearby`.  
    - `page`, `size` = pagination (default 0, 20).
  - **Response:** `PagedResponse<AgentResponse>`.
  - **AgentResponse** ina:  
    `id`, `userId`, `name`, `phone`, `email`, `profilePic`, `agentCode`, `status`, `region`, `district`, `ward`,  
    `latitude`, `longitude`, `averageRating`, `ratingCount`, `isOnline`,  
    `businessesActivated`, `totalReferrals`, `registeredAt`, n.k.

### 2.2 Kuunda ombi (na kuchagua agent)

- **POST** `/api/v1/users/me/business-requests`
  - **Body:** `CreateBusinessRequestRequest`:  
    `businessName`, `ownerPhone`, **`agentCode`** (optional; ikiwa user amechagua agent), `category`, `region`, `district`, `ward`, `street`, `description`.
  - **Response:** `BusinessRequestResponse` + `paymentOrderId` (kwa polling ikiwa unatumia).

### 2.3 Kumpa rating agent (baada ya activation)

- **POST** `/api/v1/users/me/rate-agent`
  - **Body:** `RateAgentRequest`:  
    `agentId` (UUID), `rating` (1–5), `comment` (optional).
  - **Rule:** User ana business iliyo-activate na agent huyo tu ndiyo anaweza kumpa rating.

---

## 3. UI/UX – Map na List

### 3.1 Default: Map view (option inaanza na map)

- **Header:** "Choose an agent" / "Chagua agent" na **tabs au toggle:** [Map] [List].
- **Map (e.g. Leaflet):**
  - **Markers** = agents wenye `latitude` na `longitude` si null.
  - **Filter (optional):** Onyesha **"Online only"** – `isOnline === true`.
  - **Click marker** → **popup / bottom sheet** na:
    - Photo, name, agent code.
    - Rating (stars) + `ratingCount` (e.g. "4.2 ★ (12 reviews)").
    - "X businesses activated".
    - Badge "Online" ikiwa `isOnline`.
    - Kitufe **"Request with this agent"** → kufungua fomu ya business request na `agentCode` imejaa.
- **User location (optional):** Onyesha dot ya user ikiwa umepata `lat/lng` (browser au profile) ili "Nearby" iwe na maana kwenye list.

### 3.2 List view

- **Sort dropdown:** Popularity | Rating | Nearby.
  - **Popularity:** `GET .../for-business-request?sort=popularity&page=0&size=20`.
  - **Rating:** `sort=rating`.
  - **Nearby:** `sort=nearby&lat=...&lng=...` (lat/lng kutoka profile au geolocation).
- **Card per agent:**
  - Photo, name, code.
  - Stars + rating count.
  - Businesses activated.
  - "Online" badge ikiwa `isOnline`.
  - Kitufe **"Select"** / **"Request with this agent"** → fomu na `agentCode` imejaa.

### 3.3 Fomu ya business request

- Baada ya kuchagua agent (au kuchagua "No agent" ikiwa unaruhusu):  
  Business name, Owner phone, Category, Region (optional), n.k.  
- **Hidden/read-only:** `agentCode` ikiwa amechagua agent.  
- Submit → "USSD payment push sent…" (au success message); ikiwa unatumia polling, poll **GET /api/v1/payments/status/{orderId}** na ikiwa success → refresh token + redirect/update UI (e.g. show rate popup).

### 3.4 Popup ya rating (baada ya activation)

- **Trigger:** Baada ya payment success (polling au webhook flow) na user sasa ana role BUSINESS.
- **Content:**  
  "How was your experience with [Agent name]?"  
  Stars 1–5 + optional comment box.  
  Kitufe **"Submit"**.
- **API:** **POST** `/api/v1/users/me/rate-agent` na `{ agentId, rating, comment }`.
- Baada ya success: fungua popup / toast "Thank you for your rating!".

---

## 4. Mfano wa Frontend (React-style)

### 4.1 Fetch agents (list / map)

```javascript
// sort: 'popularity' | 'rating' | 'nearby'
// lat, lng: required when sort === 'nearby'
async function getAgentsForBusinessRequest(sort = 'popularity', lat, lng, page = 0, size = 20) {
  const params = new URLSearchParams({ sort, page, size });
  if (sort === 'nearby' && lat != null && lng != null) {
    params.set('lat', lat);
    params.set('lng', lng);
  }
  const res = await api.get(`/agent/for-business-request?${params}`);
  return res.data.data; // { content, page, size, totalElements, totalPages, ... }
}
```

### 4.2 Create business request (na agent)

```javascript
async function createBusinessRequest(payload) {
  // payload = { businessName, ownerPhone, agentCode?, category?, region?, ... }
  const res = await api.post('/users/me/business-requests', payload);
  return res.data.data; // { id, paymentOrderId, ... }
}
```

### 4.3 Rate agent (popup after activation)

```javascript
async function rateAgent(agentId, rating, comment) {
  await api.post('/users/me/rate-agent', { agentId, rating, comment });
}
```

### 4.4 Map (Leaflet) – agents with coordinates

- Fetch agents (e.g. `sort=nearby` na user lat/lng, au `sort=popularity` na `size=50`) na filter client-side `a.latitude != null && a.longitude != null`.
- For each agent: `<Marker position={[a.latitude, a.longitude]} onClick={() => setSelectedAgent(a)}>`
- Popup: show `selectedAgent` details na kitufe "Request with this agent" → set `agentCode` na open form.

---

## 5. Muhtasari

- **Settings** → **Become a business** → UI nzuri: **Map** (default) na **List**.
- **Agents** wanapangwa kwa **popularity**, **rating**, au **nearby** (API tayari).
- **Map:** Onyesha agents wenye location; click → details → "Request with this agent" → fomu na `agentCode`.
- **Request** inaenda **direct kwa agent** (backend inaweka `agentCode` kwenye `BusinessRequest`).
- **Baada ya activation:** **Popup** ya kumpa **rating** agent (1–5 + comment) → **POST /api/v1/users/me/rate-agent**.

Backend iko tayari; frontend inabaki kuimplement UI/UX hapo juu. **Frontend path:** `GITHUB COOP/wakilify-ui` (Settings: `src/pages/user/Settings.jsx`, route `/app/settings`, dev server `http://localhost:5173`).
