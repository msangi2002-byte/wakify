# OTP na Onboarding Flow – Alignment na Blueprint (Facebook-like)

Hati hii inaelezea **OTP kwenye backend**, inavyolingana na **WAKILIFY Full Flow & Layer Blueprint** (User Onboarding), na hatua zinazohitajika ili mfumo uwe kama **Facebook flow** kwa UI/UX.

---

## 1. Blueprint: User Onboarding Flow (kutoka document yako)

Kutoka **WAKILIFY Full Flow & Layer Blueprint**:

1. **Open app** – Visitor anaingia.
2. **Create account** – Phone/Email + password (SSO baadaye).
3. **Verify contact** – **OTP/SMS au email code**.
4. **Create profile** – Name, photo, bio, interests.
5. **Start using Feed** – Follow, post, like, comment.

Hii ni **Facebook-like**: kujiandikisha → kuthibitisha simu/email → kukamilisha profile → kutumia app.

---

## 2. Backend OTP – Kinachofanya Kazi (Current State)

### 2.1 Kinachoendelea vizuri

| Kipengele | Status | Maelezo |
|-----------|--------|---------|
| OTP generation | ✅ | 6-digit random (100000–999999), `AuthService.generateOtp()` |
| OTP storage | ✅ | `User.otpCode`, `User.otpExpiresAt` (10 dakika) |
| Register → OTP | ✅ | Baada ya register, user ana OTP iliyohifadhiwa |
| Verify OTP | ✅ | `POST /api/v1/auth/verify-otp` (phone + otp) – inaangalia OTP na expiry, kisha `isVerified = true` |
| Forgot/Reset password OTP | ✅ | OTP inatolewa na kuthibitishwa kwa reset password |

### 2.2 Kinachokosekana (kwa sasa)

| Kipengele | Status | Maelezo |
|-----------|--------|---------|
| **Kutuma OTP kwa mtumiaji** | ❌ | Kuna `// TODO: Send OTP via SMS`. OTP inaandikwa **log tu** (`log.info("OTP for {}: {}", phone, otp)`). Mtumiaji **haipokei** OTP popote isipokuwa dev anasoma server logs. |
| **Resend OTP** | ❌ | UI spec ina "Resend code (59s)" – backend hauna endpoint ya **resend OTP**. |

**Hitimisho:** Logic ya OTP (generate, store, verify, expiry) **inafanya kazi**, lakini **delivery** (SMS/email) **haijatumika**. Kwa production, mtumiaji hawezi kuthibitisha simu bila kuunganisha SMS gateway.

---

## 3. Mfumo Unavyolingana na Blueprint (Facebook-like)

### 3.1 Flow ya API (current)

```
1. POST /auth/register     → User anaundwa, OTP inatengenezwa na kuhifadhiwa (haijatumwa kwa simu)
2. POST /auth/verify-otp   → OTP inathibitishwa → isVerified = true
3. POST /auth/login        → accessToken + refreshToken + user
4. GET /users/me           → Profile (name, photo, bio – “Create profile” inaweza kuwa PUT /users/me)
```

- **Create account** (hatua 2 ya blueprint) = register.
- **Verify contact** (hatua 3) = verify-otp (inapoendelea, OTP inatakiwa kutuma kwa SMS/email).
- **Create profile** (hatua 4) = PUT /users/me (profile, photo, bio).
- **Start using Feed** (hatua 5) = baada ya login, UI inaonyesha feed.

Kwa hiyo **muundo wa flow** unafanana na blueprint; kinachokosekana ni **kutuma OTP kwa simu/email** na **resend OTP**.

---

## 4. Unachotakiwa Kufanya Ili OTP “Ifanye Kazi Kweli” na UI/UX

### 4.1 Development / Testing (bila SMS bado)

- **Option A – Dev mode:**  
  Kwenye dev tu, **register response** inaweza kurudisha `otpForDev` (OTP iliyotumwa kwenye log). UI inaweza kuonyesha “Code: 123456” kwa testing, bila kuhitaji kusoma logs.
- **Resend OTP:**  
  Endpoint **POST /auth/resend-otp** (phone) – kuzaliwa OTP mpya, kuhifadhi, na “kusend” (kwa dev: log au kurudisha kwenye response). UI inaweza kuonyesha “Resend code (59s)” kama spec.

Hii inafanya **flow ya Facebook-like** iwe **inayofanya kazi end-to-end** kwenye dev na UI.

### 4.2 Production (SMS / Email)

- **SMS gateway:**  
  Unganisha provider (Africa’s Talking, Twilio, Infobip, n.k.) na utumie **number ya mtumiaji** kutuma OTP baada ya:
  - register
  - resend-otp
  - forgot-password
- **Email OTP (optional):**  
  Kama utatumia email kama njia mbadala ya verification, ongeza “send OTP via email” (SMTP au provider) kwa **email** wakati **phone** au **email** inatumika kwa verification.

---

## 5. Mabadiliko Yaliyopendekezwa (Backend)

1. **Resend OTP**
   - **POST /api/v1/auth/resend-otp**  
     Body: `{ "phone": "+255..." }`  
   - Backend: tafuta user kwa phone, angalia ni **unverified**, zalia OTP mpya (expiry 10 min), “send” (kwa sasa log; baadaye SMS).  
   - Rate limit: kwa mfano **si zaidi ya 1 resend kwa kila 60 sec kwa namba ile ile** (kuepuka abuse).

2. **Dev mode (optional)**
   - Property: `app.dev-mode=true` (dev only).  
   - **Register response** (dev mode only): ongeza field `otpForDev` ili UI ya dev ionyeshe OTP bila SMS.

3. **SMS integration (production)**
   - Interface kama `OtpSender.sendOtp(phone, otp)` – implementation inaita gateway yako.  
   - Wito wa `sendOtp` baada ya register, resend-otp, na forgot-password.

---

## 6. Mabadiliko Yaliyofanywa (Backend)

- **POST /api/v1/auth/resend-otp** – Body: `{ "phone": "+255..." }`. Inazaliwa OTP mpya kwa user asiye verified, inahifadhiwa (expiry 10 min). Kwa sasa inaandikwa log; production: unatumia SMS gateway.
- **Register response** – Sasa inarudisha `{ user, otpForDev? }`. `otpForDev` inaweza kurudiwa **tu** wakati `app.dev-mode=true` (application.properties), ili UI ya dev ionyeshe OTP bila SMS.
- **app.dev-mode** – Imewekwa `false` by default. Weka `true` tu kwenye dev ili kujaribu flow ya OTP kwenye UI.

Hati hii inakabili **ukweli** kwamba OTP **inafanya kazi kwenye logic** lakini **haijatumwa kwa mtumiaji**; pamoja na resend na (optional) dev OTP, flow inalingana zaidi na blueprint na UI/UX ya Facebook-like.
