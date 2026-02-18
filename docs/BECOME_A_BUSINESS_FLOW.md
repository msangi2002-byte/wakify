# Become a Business – Flow (User) – Backend na Frontend

## 1. Ukurasa na UI (Frontend)

- **Mahali:** User **Settings** (`/app/settings`).
- **Sehemu:** "Become a business" inaonekana kwa user mwenye role **USER** (si BUSINESS wala AGENT).
- **Kitendo:** K kubonyeza **"Request to become a business"** → modal inafunguka na fomu:
  - **Business name** (required)
  - **Your phone** (required) – namba ya kupokea USSD
  - **Category** (optional)
  - **Region** (optional)
- **API:** `createBusinessRequest({ businessName, ownerPhone, category?, region? })` → `POST /api/v1/users/me/business-requests`.
- **Baada ya submit:** Ujumbe "USSD payment push sent to your phone. Complete the payment to activate your business." (hakuna polling/redirect kwa sasa kama agent).

---

## 2. Backend – Mtiririko

### 2.1 Kuunda ombi (BusinessRequestService.create)

1. **User na business:**  
   - User anapatikana na anahakikishiwa **hana business** tayari (`businessRepository.findByOwnerId`).

2. **Agent (hiari):**  
   - Kama request ina `agentCode`, agent anatafutwa na kuwekwa kwenye ombi.  
   - Kama hakuna agentCode, `agent` ni `null` (inawezekana).

3. **Kumbukumbu ya ombi:**  
   - `BusinessRequest` inaundwa: user, agent (au null), businessName, ownerPhone, category, region, district, ward, street, description, status = PENDING.  
   - Inahifadhiwa.

4. **Malipo:**  
   - Kiasi: `systemSettingsService.getToBeBusinessAmount()` (kutoka Admin Settings – **system_settings**).  
   - `paymentService.initiatePayment(...)` inaitwa na:
     - type = **BUSINESS_ACTIVATION**
     - relatedEntityId = ID ya `BusinessRequest`
     - relatedEntityType = **"BUSINESS_REQUEST"**
   - **Demo mode:** Ikiwa Payment Demo Mode iko ON (admin), hakuna USSD; payment inawekwa SUCCESS na flow inaendelea (kama agent).

5. **Response:**  
   - `BusinessRequestResponse` na `paymentOrderId` (orderId ya malipo).  
   - Frontend inaweza kutumia hii kwa polling `GET /payments/status/{orderId}` (si lazima kwa sasa).

### 2.2 Malipo yamekamilika (PaymentService)

- **Webhook** (HarakaPay) au **polling** (GET /payments/status) inaweza ku-update status ya payment.  
- Ikiwa **demo mode ON:** payment inawekwa SUCCESS mara moja na `processSuccessfulPayment` inaitwa.

`activateBusiness(payment)`:

- Ikiwa `payment.getRelatedEntityType()` ni **"BUSINESS_REQUEST"** na `relatedEntityId` ipo:
  - **activateBusinessFromRequest(payment)** ndiyo inayotumika.

### 2.3 activateBusinessFromRequest (kutengeneza business na role)

1. **BusinessRequest** inatafutwa na kuthibitishwa kuwa status ni PENDING.
2. **User** tayari ana business? → status ya request inawekwa CONVERTED, kumaliza.
3. **Business** mpya inaundwa:
   - name, description, category, owner, agent (kutoka request), region, district, ward, street, status = ACTIVE.
4. **User:** `owner.setRole(Role.BUSINESS)` na kuhifadhiwa.
5. **Request:** status = CONVERTED.
6. **Commission kwa agent:** Ikiwa request ilikuwa na agent, commission (5000 TZS) inaundwa na agent anaongezewa earnings na businessesActivated.
7. **Referral bonus:** Ikiwa owner ana `referredByAgentCode` na si agent yule yule, referral bonus (2000 TZS) inaweza kuongezwa.

Baada ya hapo user ana role BUSINESS na anaweza kuingia kwenye Business Dashboard.

---

## 3. Muhtasari wa Flow (User → Business)

| Hatua | Frontend | Backend |
|--------|----------|---------|
| 1 | User anabonyeza "Request to become a business" kwenye Settings | — |
| 2 | Modal: businessName, ownerPhone, category, region → Submit | — |
| 3 | `POST /users/me/business-requests` | BusinessRequestService.create: ombi + initiatePayment (USSD au demo) |
| 4 | "USSD payment push sent…" (au demo success) | Payment PENDING (au SUCCESS ikiwa demo) |
| 5 | (Optional: poll GET /payments/status/{orderId}) | — |
| 6 | User analipa kwenye simu (au demo) | Webhook/poll → payment SUCCESS |
| 7 | — | activateBusinessFromRequest: Business + role BUSINESS + commission |
| 8 | User anaweza kuingia /business (role BUSINESS) | — |

---

## 4. Mambo muhimu

### 4.1 Fee – umekaa (iliyorekebishwa)

- **Admin Settings** inaweka **toBeBusinessAmount** kwenye **system_settings** (SystemSettingsService).
- **Become a business** sasa inatumia **SystemSettingsService.getToBeBusinessAmount()** kwenye BusinessRequestService, hivyo kiasi kinachotumika ni kile admin anachoweka kwenye Settings.

### 4.2 Agent hiari

- Fomu ya frontend **haionyeshi** uchaguzi wa agent (hakuna `agentCode`).  
- Backend inakubali `agentCode` optional; ikiwa haijatuma, commission kwa agent haitoki.

### 4.3 Polling / redirect baada ya malipo

- Frontend inaonyesha ujumbe wa "USSD push sent" lakini **ha polling** status ya payment wala redirect kwenye /business.  
- Kwa UX nzuri, unaweza kuongeza: polling `GET /payments/status/{paymentOrderId}` na ikiwa status = SUCCESS, refresh token (ili role BUSINESS iwe kwenye JWT) na redirect kwenye `/business`.

### 4.4 Demo mode

- Ikiwa **Payment demo mode** iko ON (admin), `PaymentService.initiatePayment` inaweka payment SUCCESS na kuita `processSuccessfulPayment`.  
- Hivyo "Become a business" pia inafanya **success mara moja** bila USSD halisi.

---

## 5. Hitimisho

- **Flow ya backend na frontend imekaa:** ombi linaundwa, malipo yanaanzishwa (USSD au demo), na baada ya success business inaundwa na role inasasishwa.
- **Tatizo la fee:** Kiasi kinachotumika kwa business activation kinachukuliwa kutoka **system_config**, si kutoka Admin Settings (system_settings). Inashauriwa kuunganisha na **SystemSettingsService.getToBeBusinessAmount()** ili admin aweze kudhibiti fee moja kwa moja kutoka Settings.
