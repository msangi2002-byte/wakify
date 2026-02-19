# Wakilfly Frontend – Mapitio Kamili (Full Review)

Hati hii inaelezea **frontend** kama inavyoelezewa kwenye repo ya **wakify** – na **ukweli wa path** `/GITHUB COOP/wakify`.

---

## 1. Path za Backend na Frontend

| Mradi | Path | Maelezo |
|-------|------|---------|
| **Backend** | **`/GITHUB COOP/wakify`** | Java/Spring Boot, `build.gradle`, `src/main/java`, docs |
| **Frontend (UI)** | **`/GITHUB COOP/wakilify-ui`** | React + Vite + Tailwind, `package.json`, `src/pages/user/Settings.jsx` (route `/app/settings`) |

**Wakify repo** ina backend tu; **wakilify-ui** ndiyo frontend (React, Tailwind, react-router-dom, leaflet, zustand). Settings page iko `wakilify-ui/src/pages/user/Settings.jsx`.

---

## 2. Frontend kama Inavyoelezewa kwenye Docs

Kutoka kwenye docs (hasa **WAKIFY_UI_UX_SPECIFICATION.md** na docs nyingine), frontend inaelezewa kama ifuatavyo.

### 2.1 Tech na Jina

| Kitu | Thamani |
|------|--------|
| **Jina la app** | wakilify-ui (kwenye docs) |
| **Stack** | React + Tailwind CSS |
| **Platform** | Web & Mobile (responsive) |
| **State** | Zustand au React Context (mf. authStore) |
| **API client** | Axios, base URL `https://api.wakify.co.tz/api/v1` (au env) |

### 2.2 Moduli za App (kutoka spec)

| Module | Kazi |
|--------|------|
| **Auth** | Register, Login, OTP Verification |
| **Social** | Posts, Reels, Stories, Reactions, Comments |
| **Marketplace** | Products, Orders, Cart, Reviews |
| **Live** | Streaming, Gifts, Wallet |
| **Communication** | Chat, Voice/Video Calls |
| **Business** | Business Profile, Dashboard, Products |
| **Agent** | Activation, Commissions, Withdrawals |
| **Admin** | User Management, Reports, Analytics |

### 2.3 Design System (kwenye spec)

- **Colors:** primary (blue), accent (violet), success, warning, error, gray scale.
- **Typography:** Inter / Segoe UI, sizes xs → 3xl.
- **Spacing / radius:** space-1 … space-8, rounded components.
- **Components:** Buttons (primary, secondary, danger), Inputs, Cards (post, product), Modal.

### 2.4 Screens Zilizoelezewa (wireframes kwenye spec)

- Auth: Login, Register, OTP.
- User: Home feed, Post detail, Product detail, Cart, Live list, Live viewer, Send gift, Wallet, Profile.
- Business: Dashboard, Products, Orders.
- Agent: Dashboard, Activate business, Commissions, Withdrawals.
- Admin: Dashboard, User table, Reports, n.k.

### 2.5 Muundo wa API (kwenye spec)

```
src/
├── api/
│   ├── config.js        # axios instance, base URL, Bearer token
│   ├── authService.js
│   ├── userService.js
│   ├── postService.js
│   ├── productService.js
│   ├── orderService.js
│   ├── chatService.js
│   ├── liveService.js
│   ├── giftService.js
│   └── ...
```

- **config:** `Authorization: Bearer ${token}` kutoka `localStorage.getItem('accessToken')`.
- **Mfano:** postService – getFeed, createPost, reactToPost, addComment, getStories, getReels.

### 2.6 User Flows (kwenye spec)

- **Registration:** Welcome → Register → OTP → Complete Profile → Home.
- **Purchase:** Browse → Product Detail → Add to Cart → Checkout → Payment → Confirmation.
- **Business (Agent):** Agent Login → Dashboard → Activate Business → … → Business Active.
- **Live:** Go Live → Title → Start → Viewers + Gifts → End → Summary.

### 2.7 Implementation Priority (kwenye spec)

- **Phase 1:** Auth, User Profile, Post Feed, Product Catalog.
- **Phase 2:** Stories, Reels, Comments & Reactions, Chat.
- **Phase 3:** Cart & Checkout, Orders, Business Dashboard, Agent Dashboard.
- **Phase 4:** Live Streaming, Virtual Gifts, Voice/Video Calls, Admin Panel.

---

## 3. Docs Zinazohusika na Frontend

| Doc | Maelezo |
|-----|---------|
| **WAKIFY_UI_UX_SPECIFICATION.md** | Spec kamili: UI/UX, roles, design system, screens, components, API integration, flows |
| **FRONTEND_MAP_AND_LOCATION_IMPLEMENTATION.md** | Map & location: useGeolocation, register/agent/business, admin map |
| **CHUNKED_UPLOAD_API.md** | Chunked upload (stories/reels): start → chunks → complete, mfano JS/TS |
| **STREAMING_SERVER.md** | WHEP/WHIP: direct SRS URL (`rtcApiBaseUrl` from GET /api/v1/live/config), epuka proxy 502 |
| **AGENT_SYSTEM_AUDIT.md** | Agent flow: register-agent, payment, poll getAgentMe(), refresh token + role AGENT |
| **BECOME_A_BUSINESS_FLOW.md** | Become Business: UI (settings), backend flow, frontend/backend table |
| **SOCIAL_FEATURES_API_AND_UI.md** | Social API + UI (labels, dark mode on frontend) |
| **ADMIN_PANEL_FEATURES_SPECIFICATION.md** | Admin panel features (frontend inaonyesha) |

---

## 4. Uunganishaji na Backend (kutoka docs)

- **Base URL:** Backend iko `/api/v1`; frontend inapaswa kuweka base URL (mf. env `VITE_API_URL` au `REACT_APP_API_URL`) – spec inataja `https://api.wakify.co.tz/api/v1`.
- **Auth:** Login → accessToken + refreshToken; kwa kila request header `Authorization: Bearer <accessToken>`; refresh token kwa expiry.
- **Live streaming:** Tumia **direct SRS URL** kwa WHEP/WHIP (kutoka `GET /api/v1/live/config` → `rtcApiBaseUrl`), si proxy ya backend (epuka 502).
- **Chunked upload:** POST `/api/v1/chunk-upload/start` → upload chunks → POST `.../complete` → media URLs kwa posts.
- **Agent:** Baada ya malipo, poll `GET /api/v1/agent/me`; status ACTIVE → refresh token (ili JWT ipate role AGENT) → redirect /agent.

---

## 5. Muhtasari na Mapendekezo

- **Path `/GITHUB COOP/wakify`:** Ina **backend tu** na **docs**. Frontend code **haipo** hapa.
- **Frontend (kwenye docs):** Inaelezewa kama **wakilify-ui**, React + Tailwind, na muundo/flows/API hapo juu.
- **Ikiwa unayo frontend mahali pengine:** Fungua workspace ya mradi huo (wakilify-ui au jina linalotumika) ili nipitie **code halisi** (components, routes, API calls, state).
- **Ikiwa unataka frontend ndani ya repo ya wakify:** Unaweza kuunda folder k.m. `frontend/` au `webapp/` na kuanzisha mradi wa React (Vite/CRA) + Tailwind, kisha kuunganisha na backend; naweza kukusaidia muundo wa mwanzo na env na API config.

Ukishaweka frontend (path au repo), niambie path/nama ya mradi nipitie code yake halisi.
