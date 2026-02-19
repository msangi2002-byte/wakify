# Business Registration Plans & Rate Agent – Frontend Guide (wakilify-ui)

Backend iko tayari. Hii doc inaeleza **nini cha kuongeza kwenye frontend (wakilify-ui)**.

---

## 1. Admin: Business Registration Plans (subscription fees)

**API (backend tayari):**

- **Admin CRUD** (auth required, admin with area `BUSINESS_REGISTRATION_PLANS`):
  - `GET  /api/v1/admin/business-registration-plans` – list all plans
  - `POST /api/v1/admin/business-registration-plans` – create (body: `name`, `description`, `price`, `sortOrder`, `isActive`)
  - `PUT  /api/v1/admin/business-registration-plans/{id}` – update
  - `DELETE /api/v1/admin/business-registration-plans/{id}` – delete
- **Public (for “Become a business” form):**
  - `GET /api/v1/config/business-registration-plans` – list **active** plans only (no auth)

**Frontend:**

- Ongeza **admin page** “Business registration plans” (au “Business subscriptions”):
  - Table: name, description, price, sort order, active, actions (Edit, Delete)
  - Button “Create plan” → modal/form: name, description, price, sort order, active
  - Edit: same form, pre-filled
  - Delete: confirm then call DELETE
- Ongeza **route** na **nav** chini ya admin (na permission check kwa area `BUSINESS_REGISTRATION_PLANS`).

---

## 2. “Become a business”: choose subscription plan

- Kabla ya form (au ndani ya form), **fetch** `GET /api/v1/config/business-registration-plans`.
- Onyesha **list/cards** ya plans (name, price, description); user **achagua plan moja**.
- Unapo-submit `POST /api/v1/users/me/business-request`, tumia **`businessPlanId`** (id ya plan iliyochaguliwa) pamoja na `businessName`, `ownerPhone`, `agentCode`, `latitude`, `longitude`, n.k.
- Kama user hajachagui plan, unaweza kutuma bila `businessPlanId` (backend itatumia default fee from settings).

---

## 3. Agent: Request detail – “Get directions”

- Kwenye **RequestDetail** page (agent request detail with map):
  - Ongeza button **“Get directions”** (au “Open in Maps”).
  - On click: fungua URL:
    - **Google Maps:**  
      `https://www.google.com/maps/dir/?api=1&origin={agentLat},{agentLng}&destination={userLat},{userLng}`
    - Tumia `request.agentLatitude`, `request.agentLongitude`, `request.userLatitude`, `request.userLongitude` (kama zipo).
  - Kama lat/lng haipo, weza ku-hide button au kuonyesha “Location not available”.

---

## 4. Business dashboard: first-time “Rate agent” popup

- **GET /api/v1/users/me** (user profile) inarudi:
  - `shouldRateAgent` (boolean) – true kama user ni business na bado hajarate agent aliye msajili
  - `rateAgentId` (UUID) – agent entity id kwa rate-agent API
  - `rateAgentName` (string) – jina la agent
- **Flow:**
  - Kwenye **business dashboard** (first load), piga **GET /me**.
  - Kama `user.shouldRateAgent === true`, **onyesha popup/modal**: “Rate your registration agent: {rateAgentName}”, stars 1–5, comment (optional).
  - On submit: **POST /api/v1/users/me/rate-agent** body: `{ "agentId": user.rateAgentId, "rating": 1-5, "comment": "..." }`.
  - Baada ya success, **refetch** GET /me (ili `shouldRateAgent` kuwa false) na funga popup.

---

## 5. Muhtasari

| Task | Backend | Frontend (wakilify-ui) |
|------|---------|-------------------------|
| Admin CRUD business plans | ✅ | Page + list/create/edit/delete |
| Public list active plans | ✅ GET /config/business-registration-plans | Fetch on “Become a business”, show plan picker |
| Request with plan id | ✅ businessPlanId in request, fee = plan price | Send businessPlanId when user selects plan |
| Agent “Get directions” | (coordinates already in API) | Button → Google Maps URL |
| First-time rate agent | ✅ shouldRateAgent, rateAgentId, rateAgentName in GET /me | Popup on business dashboard, then POST rate-agent |
