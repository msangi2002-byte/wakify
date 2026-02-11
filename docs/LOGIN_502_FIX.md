# Login 502 – Sababu na Marekebisho

## 502 Bad Gateway inamaanisha nini?

- **502** inatoka kwa **nginx** (au proxy nyingine), si kwenye UI.
- Inamaanisha: proxy ilitumia request kwa backend, lakini **jibu lilikuwa batili** au **backend haikujibu** (crash, timeout, connection refused).

Kwa hivyo tatizo ni **server-side**: ama backend inacrash wakati wa login, ama backend haijaendesha / imefungwa.

---

## Sababu iliyowezekana na iliyorekebishwa (backend)

### 1. **NPE kwenye login – user aliyesajiliwa na email tu** ✅ IMEREKEBISHWA

- **CustomUserDetailsService** ilitumia `user.getPhone()` kama “username” ya Spring Security.
- Kama user alisajiliwa na **email tu** (hana namba ya simu), `user.getPhone()` inaweza kuwa **null**.
- Spring Security `User` inahitaji username isiyokuwa null → **NullPointerException** → process inaweza kushindwa kujibu → nginx inatoa **502**.

**Rekebisho:**

- **CustomUserDetailsService.loadUserByUsername**: sasa “username” ni **phone** (ikiwa ipo), vinginevyo **email**, vinginevyo **id** (kamwe null).
- **loadUserEntityByUsername**: sasa inatafuta user kwa **UUID** (ikiwa string ni UUID), vinginevyo kwa **email/phone**.
- **AuthEventService.recordEvent**: ulinzi dhidi ya `parsed == null` (User-Agent).

Baada ya hii, login haitakiwi kusababisha NPE kwa sababu ya phone/email null.

---

## Vitu vya kukagua kwenye server (deployment)

502 inaweza endelea ikiwa:

1. **Backend haijaendesha**  
   - Nginx inaelekeza `/api/v1/` kwa backend; ikiwa process ya Spring haija start, nginx inapata “connection refused” na inatoa 502.

2. **Database / connection**  
   - Ikiwa DB iko down au connection string ni vibaya, login inaweza kutupa exception. GlobalExceptionHandler inapaswa kurudisha **500** (si 502), isipokuwa process inakufa kabla ya kujibu.

3. **Nginx config**  
   - Hakikisha `proxy_pass` inaelekeza kwenye port sahihi (k.m. `http://127.0.0.1:8080`).
   - Ongeza timeouts ikiwa unafikiri backend inachelewa:  
     `proxy_connect_timeout`, `proxy_send_timeout`, `proxy_read_timeout` (k.m. 60s).

4. **Logs za backend**  
   - Wakati unafanya login, angalia logs za Spring Boot.
   - Ikiwa kuna **exception** (NPE, SQL, n.k.) kabla ya kujibu, andika error ile na utumie kurekebisha.

---

## UI (wakilify-ui)

- Login inatumia **POST /api/v1/auth/login** na body: `{ emailOrPhone, password }`.
- Hii inalingana na backend (`LoginRequest`: `emailOrPhone`, `password`).
- **502** haitoki kwa makosa ya frontend; inatokana na jibu la server (au kutokujibu).

---

## Muhtasari

| Kitu | Hatua |
|------|--------|
| **NPE (phone null)** | Imerekebishwa: username sasa ni phone / email / id; loadUserEntityByUsername inasaidia UUID na email/phone. |
| **502 bado iko** | Kagua: backend inaendesha? Logs wakati wa login? Nginx proxy_pass na timeouts? DB connection? |

Baada ya deploy ya backend iliyorekebishwa, jaribu login tena. Ikiwa 502 inaendelea, tumia logs za backend na config ya nginx kujua hatua inayofuata.
