# Wakilfly Backend – Mapitio Kamili (Full Review)

Hati hii inaelezea **backend yote** baada ya mapitio: muundo, flow, mafolder, na maoni/mapendekezo.

---

## 1. Tech Stack na Mradi

| Kitu | Thamani |
|------|--------|
| **Jina** | wakilfly (Wakilfly – Social Commerce Platform) |
| **Build** | Gradle (Java 17, Spring Boot 3.2.2) |
| **Port** | 8080 |
| **Package** | `com.wakilfly` |
| **DB** | MySQL (`wakilfly_db`), JPA/Hibernate `ddl-auto=update` |
| **Auth** | JWT (access + refresh), Spring Security, BCrypt |
| **Nyingine** | HarakaPay (malipo), SRS (streaming), W-OTP (WhatsApp OTP), SMTP (email OTP), Storage VPS (SFTP) |

---

## 2. Muundo wa Mafolder (Folder Structure)

```
src/main/java/com/wakilfly/
├── WakilflyApplication.java    # Entry point, @SpringBootApplication, @EnableScheduling
├── config/                      # Configuration
│   ├── SecurityConfig.java      # CORS, stateless JWT, role-based paths
│   ├── DataInitializer.java    # CommandLineRunner: seed gifts, coin packages, system config, test user
│   └── (Storage, etc.)
├── controller/                  # REST API – 26 controllers
│   ├── AuthController          # /api/v1/auth – register, login, verify-otp, refresh, forgot-password
│   ├── UserController          # Profile, search, follow, onboarding
│   ├── PaymentController       # Initiate payment, status
│   ├── PaymentWebhookController # HarakaPay webhook
│   ├── OrderController         # Create order, my orders, business orders
│   ├── CartController          # Add/remove cart, get cart
│   ├── ProductController       # CRUD products, public GET
│   ├── BusinessController      # Business CRUD, dashboard, activation
│   ├── LiveStreamController    # Start/schedule live, list, config (HLS/WebRTC)
│   ├── SrsWebhookController    # SRS on-publish / on-unpublish
│   ├── PostController          # Posts CRUD, trending, public
│   ├── SocialController        # Follow, feed
│   ├── FriendshipController   # Friend request, accept, decline
│   ├── CommunityController    # Communities, members, polls, events
│   ├── ChatController          # Messages, conversations
│   ├── CallController          # Voice/video call metadata
│   ├── GiftController          # Send virtual gifts (coins)
│   ├── NotificationController # Notifications
│   ├── ReportController        # Report content/user
│   ├── AgentController         # Agent register, dashboard, commissions
│   ├── AdminController         # Users, businesses, agents, audit, stats (ADMIN only)
│   ├── SubscriptionController  # Plans, subscribe
│   ├── AdController            # Ads CRUD
│   ├── PromotionController     # Promotions
│   ├── PostBoostController     # Boost post
│   ├── HashtagController       # Hashtags
│   ├── ReviewController        # Product reviews
│   ├── FavoriteController      # Favorites
│   ├── ConfigController        # Public config (e.g. fees)
│   ├── HealthController        # /api/v1/health
│   ├── ChunkUploadController   # Chunked file upload (stories/reels)
│   └── StreamingProxyController # Streaming proxy
├── service/                     # Business logic (~40+ services)
│   ├── AuthService             # Register, login, OTP, refresh, forgot-password
│   ├── PaymentService          # HarakaPay initiate, webhook, process success (coins/order/subscription/agent/business)
│   ├── OrderService            # Create order, list, update status
│   ├── CartService
│   ├── ProductService
│   ├── LiveStreamService       # Create stream, verify streamKey, set offline
│   ├── PostService
│   ├── GiftService
│   ├── AgentService
│   ├── AdminService
│   ├── otp/
│   │   ├── OtpSender           # Interface (e.g. WhatsApp)
│   │   ├── WhatsAppOtpSender
│   │   └── EmailOtpSender
│   └── ... (Community, Notification, Report, Subscription, etc.)
├── repository/                  # JPA repositories (~60+)
│   ├── UserRepository
│   ├── PaymentRepository
│   ├── OrderRepository
│   ├── LiveStreamRepository
│   └── ...
├── model/                       # JPA entities (~80+)
│   ├── User, Role
│   ├── Payment, PaymentStatus, PaymentType
│   ├── Order, OrderItem, OrderStatus
│   ├── LiveStream, CartItem, Product, Business
│   ├── Post, Comment, PostReaction, Hashtag
│   ├── Community, CommunityMember, CommunityPoll
│   ├── Message, Friendship, Notification
│   ├── Agent, Commission, GiftTransaction, UserWallet
│   └── ...
├── dto/
│   ├── request/                # CreateXxxRequest, UpdateXxxRequest, LoginRequest, etc.
│   └── response/               # ApiResponse<T>, AuthResponse, OrderResponse, etc.
├── security/
│   ├── JwtAuthenticationFilter # Bearer token → validate → SecurityContext
│   ├── JwtTokenProvider        # Generate/validate JWT, username from token
│   ├── CustomUserDetailsService # loadUserByUsername (phone/email), loadUserEntityByUsername
│   └── UserPrincipal           # UserDetails implementation
├── exception/
│   ├── GlobalExceptionHandler  # @RestControllerAdvice – BadRequest, NotFound, Validation, 401, 403, 500
│   ├── BadRequestException
│   └── ResourceNotFoundException
└── util/
    └── RequestContextUtils     # Build AuthRequestContext from HttpServletRequest (IP, user-agent)
```

```
src/main/resources/
├── application.properties       # Server, DB, JWT, upload, HarakaPay, streaming, storage VPS, logging
├── application-local.properties
├── application-prod.properties
└── db/migration/                # Flyway-style migrations V2–V15 (e.g. auth_events, contact hashes, live comments)
```

---

## 3. Flow ya Mfumo (System Flow)

### 3.1 Request flow (kila API call)

1. **Client** → HTTP request na `Authorization: Bearer <accessToken>` (isipokuwa public path).
2. **JwtAuthenticationFilter** (OncePerRequestFilter):
   - Inachukua token kutoka header; ina-validate na `JwtTokenProvider`.
   - Inapata username (phone/email) kutoka token.
   - Inaload `UserDetails` kupitia `CustomUserDetailsService` (DB).
   - Inaweka `Authentication` kwenye `SecurityContextHolder`.
3. **SecurityConfig**:
   - Public: `/api/v1/auth/**`, `/api/v1/health`, `/api/v1/webhooks/**`, GET products/posts/trending/businesses/plans/agent/code|search, config/fees, uploads.
   - Authenticated: `POST /api/v1/agent/register`, GET agent/me, agent/dashboard.
   - Role: `/api/v1/agent/**` → AGENT/ADMIN, `/api/v1/business/**` → BUSINESS/ADMIN, `/api/v1/admin/**` → ADMIN.
   - `anyRequest().authenticated()` kwa zingine.
4. **Controller** → **Service** → **Repository** (na matumizi ya HarakaPay, SRS webhooks, Storage VPS, OTP).
5. **Response**: `ApiResponse<T>` (success, message, data, errors) – na `GlobalExceptionHandler` inapanga status codes (400, 401, 403, 404, 500).

### 3.2 Auth flow

- **Register:** POST `/api/v1/auth/register` → AuthService (check duplicate phone/email, create user, OTP → WhatsApp/Email), response na OTP kwa dev.
- **Verify OTP:** POST `/api/v1/auth/verify-otp` → set verified, clear OTP.
- **Login:** POST `/api/v1/auth/login` → AuthenticationManager + JWT (access + refresh) → AuthResponse.
- **Refresh:** POST `/api/v1/auth/refresh` na refreshToken → access token mpya.
- **Forgot password:** OTP kwa simu, kisha set password mpya.

### 3.3 Payment flow (HarakaPay)

- **Initiate:** POST `/api/v1/payments/initiate` → PaymentService (create Payment PENDING, call HarakaPay collect API, store transactionId); demo mode inaweza mark SUCCESS na process mara moja.
- **Webhook:** POST `/api/v1/webhooks/harakapay` → refresh status → on SUCCESS, process (coins/wallet, subscription, order, agent package, business activation).
- **Status:** GET payment status by orderId (authenticated/public endpoints).

### 3.4 Order flow

- Cart (CartController) → POST `/api/v1/orders` (CreateOrderRequest) → OrderService (Order + OrderItems, link na payment ikiwa kuna).
- Buyer: GET my orders; Business: GET orders za business, update status (CONFIRMED, SHIPPED, n.k.).

### 3.5 Live streaming flow

- POST `/api/v1/live/start` au schedule → LiveStreamService (roomId, streamKey, LiveStream entity).
- OBS/app inapush RTMP kwenye streaming server (SRS).
- SRS webhooks: on-publish → verify streamKey (accept/reject); on-unpublish → set stream offline.
- Viewers: GET live list, config (HLS/WebRTC/STUN-TURN), then play.

### 3.6 Social / Commerce / Agent / Admin

- Social: posts, follow, friendship, comments, reactions, community (polls, events).
- Commerce: business, products, cart, orders, reviews.
- Agent: register (pay package), dashboard, commissions; referral code wakati wa registration.
- Admin: CRUD users/businesses/agents, audit logs, system stats.

---

## 4. Mafaili Muhimu (Key Files)

| Kazi | Faili |
|------|--------|
| Kuanzisha app | `WakilflyApplication.java` |
| Security (paths, JWT, CORS) | `config/SecurityConfig.java` |
| JWT filter | `security/JwtAuthenticationFilter.java` |
| Token generate/validate | `security/JwtTokenProvider.java` |
| User load (auth) | `security/CustomUserDetailsService.java` |
| Seed data + test user | `config/DataInitializer.java` |
| Exception → HTTP response | `exception/GlobalExceptionHandler.java` |
| Response wrapper | `dto/response/ApiResponse.java` |
| Auth logic | `service/AuthService.java` |
| Payment + webhook | `service/PaymentService.java` |
| Config (DB, JWT, HarakaPay, streaming, storage) | `src/main/resources/application.properties` |

---

## 5. Maoni na Mapendekezo (Kitu)

Baada ya kupitia system yote, hizi ndizo pendekezo zangu:

### 5.1 Usalama na Config

- **JWT secret** – Sasa iko wazi kwenye `application.properties`. Production: tumia env variable (e.g. `JWT_SECRET`) na usiweke secret kwenye repo.
- **HarakaPay API key** – Sawa kuweka kwenye env (e.g. `PAYMENT_HARAKAPAY_API_KEY`).
- **Storage VPS password** – Sawa kuhifadhi kwenye env / secret manager, si kwenye properties file ya repo.
- **CORS** – `setAllowedOrigins(List.of("*"))` inaruhusu kila origin. Production: weka domains maalum (e.g. app.wakilfy.com).

### 5.2 Database

- **ddl-auto=update** – Inasaidia development; production ni bora kutumia **Flyway/Liquibase** na migrations (tayari una `db/migration/`). Weka `ddl-auto=validate` au `none` production.
- **show-sql=true** – Production: zima au weka DEBUG kwa package maalum tu (performance).

### 5.3 Code na Muundo

- **PaymentService** – Faili kubwa (600+ lines). Inaweza kugawanywa: `HarakaPayClient`, `PaymentProcessingService` (process successful payment by type), ili kurahisisha test na maintenance.
- **Rate limiting** – Kwenye `/api/v1/auth/register` na `/api/v1/auth/resend-otp` ni muhimu (kuepuka abuse). Ongeza rate limit (kwa IP au phone) kwa kutumia Bucket4j au Spring rate limit.
- **Validation** – DTOs zina `@Valid`; endelea kutumia validation constraints kwenye request objects ili `GlobalExceptionHandler` izitreat vizuri.

### 5.4 OTP na SMS

- Doc inasema "TODO: SMS OTP". Kwa sasa WhatsApp (W-OTP) na Email zipo. Kama production inahitaji SMS fallback, unaweza kuongeza `SmsOtpSender` implement `OtpSender`.

### 5.5 Testing

- Una `CartServiceTest` na Spring test dependencies. Ni vizuri kuongeza unit tests kwa AuthService, PaymentService (logic ya process successful payment), na integration tests kwa critical flows (auth, create order, payment webhook).

### 5.6 API na Docs

- Kama bado hauna, OpenAPI (Swagger) inasaidia frontend na third-party: `springdoc-openapi` au `springfox` kwa `/v3/api-docs` na Swagger UI.

### 5.7 Monitoring

- **Health** – `/api/v1/health` ipo. Unaweza kuongeza DB health (Spring Boot Actuator + `spring-boot-starter-actuator`) kwa production.
- **Logging** – Tayari una logging kwa wakilfly na security; production: weka level INFO kwa default, DEBUG kwa package maalum ikiwa unatatua.

---

## 6. Muhtasari

- **Muundo:** Wazi – Controllers → Services → Repositories; Security na Exception handling zimeunganishwa vizuri.
- **Flow:** Auth (JWT + OTP), Payment (HarakaPay + webhook), Order (cart → order), Live (SRS webhooks), Social/Commerce/Agent/Admin – zote zinaeleweka kutoka docs na code.
- **Mafolder:** config, controller, service, repository, model, dto (request/response), security, exception, util – na resources (properties, migrations).

Ukishataka, naweza kukusaidia kuanza na moja ya mapendekezo (kwa mfano: env vars kwa secrets, kugawa PaymentService, au rate limiting kwenye auth).
