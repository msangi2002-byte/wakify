# Frontend: Map & Location – Miongozo ya Implementation (wakilify-ui)

Backend iko tayari. Hii doc inaeleza **nini cha kuongeza kwenye frontend (wakilify-ui)** ili:
1. **Kuchukua location (automatic background)** wakati wa User register, Agent register, na Business activation – kuituma kwenye API.
2. **Admin map** – ku-fetch locations na kuonyesha map yenye icons tofauti (USER, AGENT, BUSINESS).

---

## 1. Kuchukua location (automatic) na kuituma kwenye API

### 1.1 Hook / utility ya geolocation (React)

Onda file kama `src/hooks/useGeolocation.js` (au `.ts`):

```javascript
import { useState, useEffect, useCallback } from 'react';

/**
 * Gets current position once (for registration). Use when screen mounts.
 * Returns { latitude, longitude } or null if denied/unavailable.
 */
export function useGeolocation(options = {}) {
  const [position, setPosition] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchPosition = useCallback(() => {
    if (!navigator.geolocation) {
      setError('Geolocation not supported');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setPosition({
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude,
        });
        setLoading(false);
      },
      (err) => {
        setError(err.message);
        setPosition(null);
        setLoading(false);
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 60000,
        ...options,
      }
    );
  }, []);

  useEffect(() => {
    fetchPosition();
  }, [fetchPosition]);

  return { position, error, loading, retry: fetchPosition };
}
```

### 1.2 User registration – kuongeza lat/lng kwenye payload

Wakati user anasajili (screen ya signup/register):

- Tumia `useGeolocation()` au wakati form inaposubmit, piga `navigator.geolocation.getCurrentPosition` mara moja.
- Ikiwa umepata `latitude` na `longitude`, ziongeze kwenye body ya **POST /api/v1/auth/register** (au endpoint yako ya register).

Mfano (pseudocode):

```javascript
// On register form submit
const payload = {
  name,
  phone,
  email,
  password,
  currentCity,
  region,
  country,
  // ... other fields
};

// Optional: add location if available (background)
if (position) {
  payload.latitude = position.latitude;
  payload.longitude = position.longitude;
}

await api.post('/api/v1/auth/register', payload);
```

Au kwenye mount ya registration screen, tumia hook na kuhifadhi `position` kwenye state; wakati wa submit ongeza `latitude`/`longitude` kwenye payload.

### 1.3 Agent registration – kuongeza lat/lng

Kwenye screen ya **Agent registration** (POST /api/v1/agent/register):

- Tumia `useGeolocation()` au `getCurrentPosition` wakati screen inafunguka (au kabla ya submit).
- Ongeza kwenye body:

```javascript
const payload = {
  nationalId,
  region,
  district,
  ward,
  street,
  paymentPhone,
};
if (position) {
  payload.latitude = position.latitude;
  payload.longitude = position.longitude;
}
await api.post('/api/v1/agent/register', payload);
```

### 1.4 Business activation (Agent) – kuongeza lat/lng

Kwenye screen ya **Activate Business** (POST /api/v1/agent/activate-business):

- Vile vile: pata position (automatic) au map picker, kisha ongeza kwenye body:

```javascript
const payload = {
  businessName,
  description,
  category,
  region,
  district,
  ward,
  street,
  ownerName,
  ownerPhone,
  paymentPhone,
  // ...
};
if (position) {
  payload.latitude = position.latitude;
  payload.longitude = position.longitude;
}
await api.post('/api/v1/agent/activate-business', payload);
```

### 1.5 Profile update – location (optional)

Kwenye **Update profile** (PUT /api/v1/users/me au sawa), ikiwa una field ya “location” au “map pin”:

- Ongeza `latitude` na `longitude` kwenye body ya update (kutoka geolocation au map picker).

---

## 2. Admin map – kuonyesha USER, AGENT, BUSINESS na icons tofauti

### 2.1 API

- **GET /api/v1/admin/map/locations** (Admin token required)
- Response: array ya objects, kila object:
  - `id`, `name`, `latitude`, `longitude`, `type` (`"USER"` | `"AGENT"` | `"BUSINESS"`), `region`, `category`

### 2.2 Ku-fetch na ku-display

- Kwenye Admin panel, ona route kama **Map** au **Locations Map**.
- Fetch: `GET /api/v1/admin/map/locations`.
- Tumia library ya map (k.m. **Leaflet** au **Google Maps React**).
- Kwa kila item, weka **marker** kwenye `latitude`, `longitude`.
- Chagua **icon** kulingana na `type`:
  - `type === 'USER'` → icon ya user (mf. mtu / user pin).
  - `type === 'AGENT'` → icon ya agent (mf. badge / agent pin).
  - `type === 'BUSINESS'` → icon ya business (mf. duka / shop pin).

Mfano (React + Leaflet):

```javascript
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';

// Icons (badilisha paths kwa icons zako)
const userIcon = new L.Icon({
  iconUrl: '/icons/map-user.png',
  iconSize: [28, 28],
});
const agentIcon = new L.Icon({
  iconUrl: '/icons/map-agent.png',
  iconSize: [28, 28],
});
const businessIcon = new L.Icon({
  iconUrl: '/icons/map-business.png',
  iconSize: [28, 28],
});

function getIcon(type) {
  if (type === 'AGENT') return agentIcon;
  if (type === 'BUSINESS') return businessIcon;
  return userIcon;
}

export function AdminMapPage() {
  const [locations, setLocations] = useState([]);

  useEffect(() => {
    api.get('/api/v1/admin/map/locations', { headers: { Authorization: `Bearer ${adminToken}` } })
      .then((res) => setLocations(res.data.data || []))
      .catch(console.error);
  }, []);

  return (
    <MapContainer center={[-6.3690, 34.8888]} zoom={6} style={{ height: '500px' }}>
      <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
      {locations.map((loc) => (
        loc.latitude != null && loc.longitude != null && (
          <Marker key={`${loc.type}-${loc.id}`} position={[loc.latitude, loc.longitude]} icon={getIcon(loc.type)}>
            <Popup>
              <strong>{loc.name}</strong><br />
              {loc.type} {loc.region && ` • ${loc.region}`}
            </Popup>
          </Marker>
        )
      ))}
    </MapContainer>
  );
}
```

### 2.3 Dependencies (mfano)

- Leaflet: `npm install leaflet react-leaflet`
- CSS: `import 'leaflet/dist/leaflet.css';`
- Ongeza icons (png/svg) kwa USER, AGENT, BUSINESS kwenye `public/icons/` (au asset folder yako).

---

## 3. Muhtasari – checklist kwa wakilify-ui

| Task | Maelezo |
|------|---------|
| **useGeolocation hook** | Onda hook au util ya kupata `latitude`/`longitude` (background) |
| **Register screen** | Ongeza lat/lng kwenye payload ya POST register ikiwa position ipo |
| **Agent register screen** | Ongeza lat/lng kwenye payload ya POST /api/v1/agent/register |
| **Activate business screen** | Ongeza lat/lng kwenye payload ya POST /api/v1/agent/activate-business |
| **Profile update** | Optional: ongeza lat/lng kwenye update profile ikiwa una location field |
| **Admin map page** | Fetch GET /api/v1/admin/map/locations, map na Leaflet/Google Maps, icon tofauti kwa USER / AGENT / BUSINESS |
| **Map icons** | Ongeza icons 3: user, agent, business (png/svg) kwa markers |

Backend iko tayari; updates zote za location na map zinahitaji kufanywa **kwenye UI (wakilify-ui)** kwa kufuata miongozo hapa juu.
