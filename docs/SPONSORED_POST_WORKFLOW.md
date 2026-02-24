# Sponsored Post – Logic and Workflow

## Overview

When a user **sponsors (boosts)** a post, the system creates a **promotion** and either charges via USSD or, if admin has enabled “all sponsored free”, activates it without payment.

---

## 1. User boosts a post (app)

- **Screen:** Boost page (`/app/boost`).
- User selects post, objective (Engagement / Messages / Traffic), audience (Automatic / Local), budget (target reach), and payment phone.
- **API:** `POST /api/v1/ads/boost-post` (body: `postId`, `targetReach`, `paymentPhone`, `objective`, `audienceType`, `targetRegions`, `ctaLink`, etc.).
- **Backend:** `PostBoostService.createPostBoost()`.

---

## 2. Backend: create promotion

- **PostBoostService** validates post ownership and target reach, computes price (ads price per person × target reach).
- Creates a **Promotion** record:
  - Type: `POST`, targetId = postId, status: `PENDING`, reach = targetReach, start/end dates, objective, audienceType, ctaLink, etc.
- **Two paths:**

### A. Sponsored free mode ON (admin toggle on Promotions page)

- **System setting:** `sponsored_free_mode = true`.
- Promotion is saved with **isPaid = true** and **status = ACTIVE**.
- No payment record or USSD is created.
- Response tells the user the promotion is live and sponsored content is currently free.

### B. Sponsored free mode OFF (normal paid flow)

- **PaymentService.initiatePayment()** is called (USSD push to the given phone).
- A **Payment** record is created and linked to the promotion (`paymentId`).
- Response returns `orderId` and message to complete payment via USSD.

---

## 3. Payment (paid flow only)

- User completes USSD payment on their phone.
- When payment succeeds, **PaymentService** (e.g. webhook or polling) marks the payment as SUCCESS and calls **activatePromotion(payment)**.
- **PromotionService.confirmPayment(promotionId)**:
  - Sets promotion **isPaid = true**.
  - If start date is not in the future, sets status to **ACTIVE**.

---

## 4. When the post appears in feed (sponsored)

- **Feed API:** `GET /api/v1/posts/feed` (or public feed).
- **PostService.getFeed()**:
  1. Loads **active promotions** (type POST, ACTIVE, not past end date, impressions &lt; reach).
  2. Filters by audience (AUTOMATIC / LOCAL / etc.) for the current user.
  3. Loads the **boosted posts** (post IDs from those promotions).
  4. Scores and sorts feed candidates (boosted posts get higher priority).
  5. **Mixes** boosted posts into the feed (e.g. every 3–5 normal posts).
  6. For each post on the page, if it is boosted, sets on **PostResponse**:
     - **isSponsored = true**
     - **promotionId**
     - **sponsorCtaLink** (from promotion’s ctaLink)
  7. **Post date/time** in the response is the **post’s real createdAt** (original post creation time).
  8. Tracks **impressions** for each boosted post shown.

So after sponsoring, the logic is: **create promotion → (if paid) complete payment → promotion goes ACTIVE → feed includes that post with isSponsored and real post date.**

---

## 5. UI

- **Feed (Home):** Sponsored posts show a “Sponsored” label and optional CTA button; time shown is the **real post time** (from `createdAt`).
- **Sidebar “Sponsored” block:** Shows active **ads** (from AdService/getActiveAds), not the same as boosted posts; dates there are the **ad’s real createdAt** when available.
- **Boost page:** User can see analytics (impressions, clicks, CTR, spent) and manage campaigns (pause/resume/cancel).

---

## 6. Admin

- **Admin → Promotions:** List all promotions, filter by status/type, pause/resume/approve/reject.
- **Toggle “All sponsored free”:** When ON, all new boosts/promotions are free and go ACTIVE immediately; when OFF, users pay via USSD and promotions activate after payment.
