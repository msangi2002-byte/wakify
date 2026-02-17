# Map Location – Jinsi Inavyofanya Kazi

**Frontend (UI) updates:** Backend iko tayari; **hakuna mabadiliko ya UI kwenye repo ya wakify**. Frontend iko kwenye **wakilify-ui**. Orodha kamili ya nini cha kuongeza kwenye UI (geolocation, map, icons) iko hapa: **[FRONTEND_MAP_AND_LOCATION_IMPLEMENTATION.md](./FRONTEND_MAP_AND_LOCATION_IMPLEMENTATION.md)**.

---

## Swali: Location inachukuliwa background au user anaona?

**Jibu:** Inaweza kuwa **automatic (background)** – yaani app inapata GPS bila user kuandika chochote. Hiyo ndiyo inayopendekezwa.

---

## Pendekezo: Automatic background wakati wa registration

- **User (kijisajili):** App ina-request location permission (mara moja), inapata `latitude` na `longitude` kiotomatiki, inatuma kwenye **POST /register**. User hajaoni au kufanya kitu – inachukuliwa background.
- **Agent (kusajili agent):** Vivyo hivyo – wakati wa agent registration, app inapata location (background), inatuma kwenye **POST /api/v1/agent/register**.
- **Business (agent activate business):** Wakati agent anasajili business, app inaweza kupata location ya mahali (au agent atumie map picker), inatuma kwenye **POST /api/v1/agent/activate-business**.

**Backend:** `latitude` na `longitude` ni **optional**. Ikiwa app itatuma – tunahifadhi. Ikiwa haitatuma – tunabaki na `null` (mtu/business haonekani kwenye map hadi location itakapojazwa baadaye kwenye profile).

---

## Chaguo mbili kwa frontend

| Njia | Maelezo |
|------|---------|
| **1. Automatic (background)** | Wakati user/agent anafungua screen ya registration, app inaomba ruhusa ya location, inapata lat/lng (GPS), inatuma kwenye API. User haoni eneo – inachukuliwa peke yake. **Hii inapendekezwa.** |
| **2. Kuonyesha na kuthibitisha** | App inapata location, inaonyesha kwenye map (pin), user anaona na anaweza kukubali au kubadilisha. Halafu inatumwa kwenye API. |

---

## API – fields za location

- **User register:** `RegisterRequest` – `latitude`, `longitude` (optional).  
- **User profile update:** `UpdateProfileRequest` – `latitude`, `longitude` (optional).  
- **Agent register:** `AgentRegistrationRequest` – `latitude`, `longitude` (optional).  
- **Business activation:** `BusinessActivationRequest` – `latitude`, `longitude` (optional).  

Zote ni optional. Ikiwa frontend inatuma – backend inahifadhi na point inaonekana kwenye admin map (icon kulingana na type: USER, AGENT, BUSINESS).

---

## Map API (admin)

- **GET /api/v1/admin/map/locations** – inarudisha point zote zenye coordinates:
  - `type: "USER"` → icon ya user
  - `type: "AGENT"` → icon ya agent  
  - `type: "BUSINESS"` → icon ya business

---

## Muhtasari

- **Location inachukuliwa automatic (background)** wakati wa registration ikiwa frontend ina-request location na kuituma kwenye API.
- **Si lazima** user aonyeshe au aandike – inaweza kuwa background.
- **Backend iko tayari** – inapokea na kuhifadhi lat/lng wakati wowote (register au profile update).
