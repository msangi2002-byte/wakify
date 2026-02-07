# Wakilfly Backend – Muundo na Flow (Architecture & Flow)

Hati hii inaelezea **backend yote** ya Wakilfly na **flow zake** ili iwe wazi kabisa.

---

## 1. Muundo wa Mradi (Project Structure)

- **Tech stack:** Spring Boot 3.2, Java 17, MySQL, JWT, Spring Security, JPA/Hibernate.
- **Jina la app:** `wakilfly` (port 8080).
- **Package:** `com.wakilfly`.

### Mafaili muhimu

| Sehemu | Path | Kazi |
|--------|------|------|
| Main app | `WakilflyApplication.java` | Kuanzisha Spring Boot + `@EnableScheduling` |
| Config | `config/` | Security, CORS, Storage, DataInitializer |
| Controllers | `controller/` | REST API – kila kitu kina `@RestController` + `@RequestMapping` |
| Services | `service/` | Biashara logic (auth, payment, orders, live, n.k.) |
| Repositories | `repository/` | JPA repositories (DB) |
| Models/Entities | `model/` | Tables za DB (User, Order, Payment, LiveStream, n.k.) |
| DTOs | `dto/request/`, `dto/response/` | Request/Response objects |
| Security | `security/` | JWT filter, token provider, UserDetailsService |
| Exception | `exception/` | Custom exceptions + `GlobalExceptionHandler` |

---

## 2. Flow ya Authentication (JWT + OTP)

### 2.1 Kuingia kwenye API (Request flow)

1. **Client** anatuma request na header: `Authorization: Bearer <accessToken>`.
2. **`JwtAuthenticationFilter`** (kabla ya controller):
   - Inachukua token kutoka header.
   - Ina-validate na **`JwtTokenProvider`** (signature + expiry).
   - Inapata **username** (phone) kutoka token.
   - Inaload **UserDetails** kupitia **`CustomUserDetailsService`** (email au phone).
   - Inaweka **Authentication** kwenye **SecurityContextHolder**.
3. **SecurityConfig** inaamua nani anaweza kufikia endpoint:
   - **Public:** `/api/v1/auth/**`, `/api/v1/health`, webhooks, baadhi ya GET (products, posts, n.k.).
   - **Authenticated:** `anyRequest().authenticated()`.
   - **Role-based:** `/api/v1/admin/**` → ADMIN, `/api/v1/business/**` → BUSINESS, `/api/v1/agent/**` → AGENT/ADMIN.
4. **Controller** inapata user kupitia `@AuthenticationPrincipal UserDetails`; service inaweza kupata `User` entity kupitia `CustomUserDetailsService.loadUserEntityByUsername(username)`.

### 2.2 Login flow

1. **POST /api/v1/auth/login** (email/phone + password).
2. **AuthService.login():**
   - `AuthenticationManager.authenticate()` (password check).
   - `JwtTokenProvider.generateAccessToken()` + `generateRefreshToken()`.
   - Inarudisha **AuthResponse** (accessToken, refreshToken, user).
3. Client huhifadhi token; kwa kila request anaweka `Authorization: Bearer <accessToken>`.

### 2.3 Registration + OTP flow

1. **POST /api/v1/auth/register** → **AuthService.register():**
   - Check phone/email si duplicate.
   - Generate OTP, weka `user.otpCode`, `user.otpExpiresAt`.
   - Save user (password encrypted with BCrypt).
   - (TODO: SMS OTP – kwa sasa log tu.)
2. **POST /api/v1/auth/verify-otp** → **AuthService.verifyOtp():**
   - Check OTP na expiry.
   - `user.setIsVerified(true)`, clear OTP, save.

### 2.4 Zingine

- **Refresh:** POST `/api/v1/auth/refresh` na `refreshToken` → access token mpya.
- **Forgot/Reset password:** OTP kwa simu, kisha set password mpya (TODO: SMS).
- **Logout:** Stateless; client inafuta token.

---

## 3. Flow ya Malipo (Payment – HarakaPay)

### 3.1 Kuanzisha malipo (initiate)

1. **POST /api/v1/payments/initiate** (authenticated).
   - Body: amount, type (e.g. COINS, ORDER, SUBSCRIPTION), phone, description.
2. **PaymentService.initiatePayment():**
   - Anatumia **Payment** entity (user, amount, type, status=PENDING, phone).
   - Anaita **HarakaPay API** (`payment.harakapay.base-url` + `/api/v1/collect`) na API key.
   - Anapokea **order_id** (transaction id), anaweka kwenye `Payment.transactionId`.
   - Inarudisha **orderId** kwa client.
3. Mtumiaji anafuata maelekezo (e.g. USSD) kukamilisha malipo kwenye simu.

### 3.2 Webhook na status

1. **HarakaPay** inaweza kutuma callback **POST /api/v1/webhooks/harakapay** (payload na `order_id`).
2. **PaymentWebhookController** inaita **PaymentService.refreshPaymentStatus(orderId)**.
3. **PaymentService** inaweza ku-poll/refresh status kutoka HarakaPay (kama kuna API ya status), kisha update **Payment** (status, paidAt, n.k.).
4. Baadhi ya malipo (e.g. COINS, SUBSCRIPTION) baada ya SUCCESS – service inaweza ku-update **UserWallet**, **Subscription**, **Order**, n.k. (logic iko ndani ya PaymentService).

### 3.3 Kuangalia status (client)

- **GET /api/v1/payments/status/{orderId}** (authenticated) – refresh status + return.
- **GET /api/v1/webhooks/payment/status/{orderId}** – public (e.g. kwa app).
- **POST /api/v1/webhooks/payment/refresh/{orderId}** – manual refresh.

---

## 4. Flow ya Order (Soko / E‑commerce)

### 4.1 Kutoka Cart → Order

1. Mtumiaji ana **Cart** (CartItem = product + quantity). Endpoints: CartController (add, remove, get).
2. **POST /api/v1/orders** (CreateOrderRequest – e.g. cart items / product IDs + quantities, shipping).
3. **OrderService.createOrder(userId, request):**
   - Anatumia Cart/Product data kuunda **Order** + **OrderItem**.
   - Order status (e.g. PENDING).
   - Anaweza kuhusisha na **Payment** (type=ORDER) – initiate payment au link na payment iliyokwisha.

### 4.2 Buyer vs Business

- **Buyer:** GET `/api/v1/orders/my` (orders za user), GET `/api/v1/orders/{id}`.
- **Business (duka):** GET orders za business yake, update status (e.g. CONFIRMED, SHIPPED) – **OrderController** + **OrderService** (role BUSINESS/ADMIN).

---

## 5. Flow ya Live Streaming (SRS)

### 5.1 Kuanza live / ku-schedule

1. **POST /api/v1/live/start** au **POST /api/v1/live/schedule** (authenticated).
2. **LiveStreamService.createLiveStream(hostId, title, description, scheduledAt):**
   - Check host haana live nyingine active.
   - Anatengeneza **roomId** (e.g. `live_xxx`), **streamKey** (unique).
   - Anaunda **LiveStream** (status = LIVE au SCHEDULED), anahifadhi DB.
   - Inarudisha **LiveStreamResponse** (streamKey, rtmp url, hls url, webrtc config kutoka application.properties).

### 5.2 Streaming server (nje ya backend hii)

- **RTMP:** App (OBS/n.k.) inapush kwenye `streaming.rtmp-url` + streamKey.
- **SRS** (streaming server) inapokea publish; inaweza kutuma **webhook** kwa backend.

### 5.3 SRS Webhooks (backend)

1. **POST /api/v1/streams/on-publish** (na token param):
   - **SrsWebhookController** inapokea payload (stream = streamKey).
   - Inaita **LiveStreamService.verifyStreamKey(streamKey)** – check streamKey iko kwenye DB na stream inaruhusiwa.
   - Inarudisha `0` (accept) au `1` (reject).
2. **POST /api/v1/streams/on-unpublish**:
   - **LiveStreamService.setStreamOffline(streamKey)** – update status stream kuwa offline/ended.

### 5.4 Watumiaji (viewers)

- **GET /api/v1/live** (list live/scheduled).
- **GET /api/v1/live/config** – STUN/TURN, HLS, WebRTC URLs (kutoka application.properties).
- Watch: HLS/WebRTC URLs zinatumika na frontend/player.

---

## 6. Flow Nyingine (Brief)

### 6.1 Social

- **Posts:** PostController + PostService (create, get, trending, public).
- **Follow:** User ↔ User (followers/following).
- **Friendship:** FriendshipController (request, accept, decline – status PENDING/ACCEPTED).
- **Comments, reactions:** Post, Comment, PostReaction.
- **Community:** CommunityController (groups/channels), CommunityMember, roles.

### 6.2 Commerce (Business)

- **Business:** BusinessController – register duka (role BUSINESS), products, dashboard.
- **Products:** ProductController (CRUD, public GET).
- **Cart:** CartController (add/remove/get).
- **Reviews:** ReviewController (product reviews; public GET).

### 6.3 Agent & Referral

- **Agent:** AgentController – register agent (authenticated user), code, search. Commissions (CommissionRepository, AgentService).
- **User.referredByAgentCode** – referral tracking wakati wa registration.

### 6.4 Gifts & Wallet

- **Virtual gifts:** GiftController, GiftService – send gift (coins), GiftTransaction.
- **UserWallet:** Coins balance; PaymentService/other services update balance.
- **Coin packages:** DataInitializer inaseed; purchase via payment (type COINS).

### 6.5 Ads, Promotions, Subscriptions

- **Ad:** AdController, AdService (create, list, status).
- **Promotion:** PromotionController.
- **Subscription:** SubscriptionController (plans, subscribe – often linked na Payment).

### 6.6 Calls, Chat, Notifications, Reports

- **Call:** CallController (voice/video call metadata – Call entity, CallStatus).
- **Chat:** ChatController (Message, conversations).
- **Notifications:** NotificationController.
- **Report:** ReportController (content/user reports).

### 6.7 Admin

- **AdminController:** CRUD users, businesses, agents; audit logs; system stats (role ADMIN only).

---

## 7. Config (application.properties) – Summary

| Kategoria | Mfano | Kazi |
|-----------|--------|------|
| Server | port 8080 | Spring app |
| DB | MySQL `wakilfly_db`, JPA ddl-auto=update | Hibernate update schema |
| JWT | jwt.secret, expiration, refresh-expiration | Access/refresh token |
| File upload | upload.path=uploads, max 10MB | Multipart |
| HarakaPay | payment.harakapay.api-key, base-url | Payment gateway |
| Streaming | rtmp-url, hls-url, webrtc-signal-url, STUN/TURN | Live stream URLs na credentials |
| Storage VPS | storage.vps.* | SFTP/upload kwa storage server |
| Scheduling | task.scheduling.pool.size | Scheduled tasks |

---

## 8. Exception Handling

- **GlobalExceptionHandler** (@RestControllerAdvice):
  - BadRequestException → 400.
  - ResourceNotFoundException → 404.
  - MethodArgumentNotValidException / ConstraintViolationException → 400 + validation errors.
  - BadCredentialsException / AuthenticationException → 401.
  - AccessDeniedException → 403.
  - Exception → 500.
- Responses zote kwa fomu **ApiResponse&lt;T&gt;** (success, message, data, errors).

---

## 9. Flow Diagram (High Level)

```
[Client]
   |
   |  Authorization: Bearer <JWT>
   v
[JwtAuthenticationFilter] --> validate token --> [CustomUserDetailsService] --> DB User
   |
   v
[SecurityConfig] --> allow/deny by path & role
   |
   v
[Controller] --> [Service] --> [Repository] --> MySQL
   |                |
   |                +--> External APIs (HarakaPay, SRS webhooks, Storage VPS)
   v
[ApiResponse] --> Client
```

---

## 10. Startup & Data

- **WakilflyApplication** inaanza Spring Boot; **@EnableScheduling** kwa scheduled tasks (e.g. PaymentService status refresh, cleanup).
- **DataInitializer** (CommandLineRunner): inaseed **VirtualGift** na **CoinPackage** kama tables ziko tupu.

---

Hii ndiyo **muundo na flow** za backend zilizosomwa. Kama unahitaji kina kwa moduli fulani (auth, payment, order, live, n.k.), niambie moduli gani.
