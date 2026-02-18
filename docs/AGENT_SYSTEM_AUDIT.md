# Agent System – Ukaguzi na Matatizo / Solutions

## 1. Mfumo wa Agent – Mwongozo

### Backend (wakify)
- **AgentController** – `/api/v1/agent/*` (register, me, dashboard, packages, activate-business, commissions, withdrawals, …).
- **Security:** POST /register, GET /registration-packages, GET /me, GET /dashboard → `.authenticated()`. Zingine → `hasAnyRole("AGENT", "ADMIN")`.
- **Registration:** POST /register inaweza kuwa na `packageId`. Kama ipo: Agent (PENDING) + Payment (AGENT_PACKAGE) + HarakaPay USSD; bila packageId: legacy (Payment AGENT_REGISTRATION, hakuna USSD hapa).
- **Malipo:** HarakaPay inatumika. Webhook: POST /api/v1/webhooks/harakapay (order_id). Pia kuna scheduled job inayocheck pending payments.
- **Activation:** PaymentService.processSuccessfulPayment() → AGENT_PACKAGE → activateAgentPackage() (activate agent + assign package); AGENT_REGISTRATION → activateAgent().

### Frontend (wakilify-ui)
- **Register Agent:** /app/register-agent – packages → form → submit → “Check your phone” → poll getAgentMe() until ACTIVE → redirect /agent.
- **Agent routes:** /agent (Dashboard, Requests, Activate, Commissions, Withdrawals) – RoleGuard allowedRoles [AGENT].
- **Menu:** “Become agent” (left + grid) kwa wasio agent; “Agent Dashboard” kwa agents.

---

## 2. Matatizo Yaliyogunduliwa na Ufumbuzi

### A. JWT haijasasishwa baada ya kuwa Agent (kubwa)
**Shida:** Baada ya malipo, frontend inaweka `role: AGENT` kwenye store na kutoa redirect kwenye /agent. JWT ya mtu bado ina role ya zamani (USER). GET /me na GET /dashboard zimeruhusiwa kwa `.authenticated()` hivyo zinaenda. Lakini Requests, Activate, Commissions, Withdrawals zinatumia endpoints zenye `hasRole('AGENT')` → 403.

**Ufumbuzi:** Baada ya kupata agent.status === ACTIVE, piga **refresh token** (POST /auth/refresh) ili kupata access token mpya yenye role sahihi kutoka DB, kisha weka tena auth store (user + token) kabla ya redirect. (Imetumika kwenye RegisterAgent.)

---

### B. Webhook ya malipo iko chini ya AgentController (path mbaya)
**Shida:** AgentController ina `@PostMapping("/webhooks/payment-callback")` chini ya `@RequestMapping("/api/v1/agent")` → URL halisi ni **/api/v1/agent/webhooks/payment-callback**. Webhook halisi ya HarakaPay iko kwenye PaymentWebhookController: **POST /api/v1/webhooks/harakapay**. Hivyo callback kwenye AgentController ni path tofauti na haijatumiki na mtiririko wa HarakaPay.

**Ufumbuzi:** Tumia webhook moja: **POST /api/v1/webhooks/harakapay**. PaymentWebhookController inaita `refreshPaymentStatus(orderId)`; PaymentService ina processSuccessfulPayment() (pamoja na activateAgentPackage). Endpoint ya AgentController `/webhooks/payment-callback` inaweza kuondolewa au kuachwa kwa provider tofauti ikiwa utahitaji baadaye.

---

### C. Polling ya status baada ya “Continue to payment”
**Shida:** RegisterAgent ina-poll **getAgentMe()** tu. Status inabadilika kuwa ACTIVE tu wakati payment ime-confirm (kwa webhook au refresh). Kama webhook haijafika, status inaweza kubaki PENDING hadi scheduled job ifanye refresh.

**Ufumbuzi:** Piga **GET /api/v1/payments/status/{orderId}** (au checkPaymentStatus(orderId)) kwa kipindi fulani. Backend inafanya refresh kutoka HarakaPay na kushughulikia success (processSuccessfulPayment). Baada ya hapo getAgentMe() inarudisha ACTIVE. Kwa UX nzuri, polling inaweza kuanza na checkPaymentStatus(orderId); status === 'SUCCESS' → refresh token + setAuth + redirect.

---

### D. User ana role AGENT lakini hakuna kipande Agent (data inconsistency)
**Shida:** Inaweza kutokea (mfano admin amempa role AGENT bila kusajili agent, au kosa wakati wa usajili). Dashboard inarudisha 404 “Agent not found”.

**Ufumbuzi (tayari):** Frontend inaonyesha ujumbe wazi na CTA “Register as agent” → /app/register-agent. Backend inarudisha ResourceNotFoundException (404). Kwa data: usiweke role AGENT bila kucreate agent record; kwa admin, tengeneza mtiririko wa “create agent” unao-create record na role.

---

### E. Duplicate / conflicting webhook
**Shida:** AgentController.paymentCallback inatarajia `transactionId` na `status` kwenye body; PaymentWebhookController.harakapay inatarajia `order_id`. HarakaPay inatumia order_id. Hivyo callback ya AgentController hailingani na HarakaPay.

**Ufumbuzi:** Orodha ya webhooks: (1) POST /api/v1/webhooks/harakapay – tumia hii kwa HarakaPay. (2) /api/v1/agent/webhooks/payment-callback – ondoa au uache kwa provider tofauti ikiwa inahitajika.

---

### F. Package info kwenye dashboard (tayari)
**Ufumbuzi:** AgentService.getAgentDashboard() inajaza packageId, packageName, packageMaxBusinesses, packageRemainingBusinesses kutoka agent.getAgentPackage().

---

### G. Street haijahifadhiwa kwenye Agent
**Shida:** AgentRegistrationRequest ina `street` lakini entity Agent haina column street.

**Ufumbuzi:** Au ongeza column `street` kwenye `agents` na uweke thamani kutoka DTO, au uondoe street kwenye DTO ikiwa haitumiwi.

---

## 3. Mabadiliko Yanayopendekezwa (kwa code)

1. **RegisterAgent (frontend):** Baada ya ACTIVE, piga refreshTokens() kisha setAuth(..., role: ROLES.AGENT) na token mpya, kisha redirect /agent.
2. **Optional:** Poll checkPaymentStatus(orderId) pamoja na getAgentMe() ili ku‑trigger refresh ya payment na activation haraka.
3. **Backend:** Ondoa au uache comment kwenye AgentController.paymentCallback; thibitisha kuwa HarakaPay inaelekezwa kwenye POST /api/v1/webhooks/harakapay.
4. **Optional:** Endpoint ya refresh iwe inarudisha user object (na role sahihi) ili store iwe consistent bila ku-hit API nyingine.

---

## 4. Mtiririko Sahihi (summary)

1. User anachagua package → anajaza form → Submit → Backend: Agent (PENDING) + Payment (AGENT_PACKAGE) + HarakaPay USSD.
2. User analipa kwenye simu (M-Pesa/Tigo/Airtel).
3. HarakaPay inatumia POST /api/v1/webhooks/harakapay (au frontend ina-poll GET /payments/status/{orderId}); backend ina-update payment na processSuccessfulPayment → activateAgentPackage → agent ACTIVE + package assigned.
4. Frontend ina-poll getAgentMe() (na optional checkPaymentStatus); status === ACTIVE → refresh token → setAuth with new token + role AGENT → redirect /agent.
5. Kwenye /agent, JWT ina AGENT → endpoints zote za agent zinaenda; dashboard inaonyesha balance, package, quick actions.
