# Wakify – Overview ya Updates (Chat Summary)

Muhtasari wa mabadiliko yote tuliyofanya kwenye backend.

---

## 1. Register (Usajili)

**Lengo:** Kukusanya data ya msajili (required + optional kwa discovery na suggested friends).

| Field | Lazima? | Maelezo |
|-------|--------|---------|
| name | Ndiyo | 2–100 chars |
| email | Ndiyo | Email valid |
| phone | Ndiyo | +/namba 10–15 |
| password | Ndiyo | Min 6 chars |
| currentCity | Si lazima | Mji – zitumika kwa people nearby |
| region | Si lazima | Mkoa |
| country | Si lazima | Taifa |
| dateOfBirth | Si lazima | Umri |
| interests | Si lazima | Hobbies comma-separated |
| referralCode | Si lazima | Agent referral code |

**API:** `POST /api/v1/auth/register` – body: RegisterRequest. User + AuthEvent (REGISTRATION), background data (IP, device, browser, OS). OTP kwa phone/email. currentCity, region, country, dateOfBirth, interests zinahifadhiwa na **zitumika** kwenye suggested + nearby.

---

## 2. Profile (Wasifu) – Features

**Lengo:** Kuonyesha na kusasisha wasifu, privacy, block/restrict, followers/following.

**Profile fields (UpdateProfileRequest / UserResponse):** Basic (name, bio, profilePic, coverPic). Location (currentCity, region, country, hometown). Details (work, education, interests, relationshipStatus, gender, dateOfBirth, website). Privacy: **profileVisibility** (PUBLIC / FOLLOWERS / PRIVATE), **followingListVisibility** (PUBLIC / FOLLOWERS / PRIVATE).

**Profile APIs:** `GET /api/v1/users/me`, `PUT /api/v1/users/me`, `POST /api/v1/users/me/avatar`, `POST /api/v1/users/me/cover`, `GET /api/v1/users/{userId}` (kwa profileVisibility), `GET /api/v1/users/me/login-activity`.

**Block/restrict:** `POST/DELETE /api/v1/users/{userId}/block`, `GET /api/v1/users/me/blocked`; `POST/DELETE /api/v1/users/{userId}/restrict`, `GET /api/v1/users/me/restricted`. **Follow:** `POST/DELETE /api/v1/users/{userId}/follow`, `GET .../followers`, `GET .../following`. followersCount, followingCount, postsCount kwenye UserResponse.

---

## 3. Discovery (Watu Karibu / Suggested)

**Lengo:** Data za register & profile (location) → suggested + nearby.

| Nini | Mabadiliko |
|------|------------|
| GET /api/v1/users/suggested | Watu same region & country (kutoka profile/register). |
| GET /api/v1/users/nearby | Watu same country; order: city → region → country. |
| Repository | UserRepository: findSuggestedUsers, findNearbyUsers. |

---

## 4. People You May Know (Facebook-Style)

**Lengo:** Contact sync + scoring (contact + location + mutuals + interests).

| Nini | Mabadiliko |
|------|------------|
| **Upload contacts** | `POST /api/v1/users/me/contacts` – body: `phones[]`, `emails[]`. Zinahifadhiwa **hashed** (SHA-256); response = watu wa Wakify ambao namba/email yao iko kwenye contacts. |
| **Unified feed** | `GET /api/v1/users/people-you-may-know` – list iliyopimwa: **contact match** (weight 10) + **location** (city/region/country, weight 3) + **mutual follows** (weight 2) + **interest overlap** (weight 1). |
| **Backend** | `UserContactHash` entity, `UserContactHashRepository`, `ContactHashUtil`, `PeopleYouMayKnowService`. `UserRepository`: `findCandidatesForPeopleYouMayKnow`, `countMutualFollowing`. |
| **Migration** | **V3** – table `user_contact_hashes`. |

---

## 5. Feed, Story & Reels Algorithms

**Lengo:** Feed si chronological tu; story na reels zina ranking kulingana na engagement/closeness.

| Feature | Mabadiliko |
|--------|------------|
| **Post feed** | Score = **recency** (decay) + **engagement** (reactions, comments, shares) + **relationship** (mutual follows, reactions/comments zako kwa mwandishi). Order: score kubwa kwanza. `findFeedCandidatesSince` (siku 14), `scoreAndSortFeedPosts`. |
| **Story** | Order: **closeness** (mara ngapi umetazama story za mwandishi) → **recency**. `StoryViewRepository.countByViewer_IdAndPost_Author_Id`. |
| **Reels** | Content: wewe + following + public. Score = **engagement** (reactions, comments, views) + **recency**. `findReelsCandidatesForUser`, `scoreAndSortReels`. |
| **Repository** | `PostRepository`: `findFeedCandidatesSince`, `findReelsCandidatesForUser`. `PostReactionRepository`: `countByUser_IdAndPost_Author_Id`. `CommentRepository`: `countByAuthor_IdAndPost_Author_Id`. |

---

## 6. Live Stream (Comments, Like, Join, Gifts, Withdraw)

**Lengo:** Comment kwenye live, like, join host, gifts na host kuconvert pesa.

| Nini | Mabadiliko |
|------|------------|
| **Comments** | `POST /api/v1/live/{liveId}/comments` (body: `content`), `GET /api/v1/live/{liveId}/comments`. `LiveStreamComment` entity; `commentsCount` inapanda. |
| **Like** | `POST /api/v1/live/{liveId}/like` – tayari ilikuwepo (likes inapanda). |
| **Join host** | Request → host anaona list → accept/reject – tayari ilikuwepo. |
| **Gifts** | Kununua coins (Haraka Pay) → send gift → host anapata 70% kwenye cash balance. `GET /api/v1/gifts/live/{liveStreamId}` – list ya gifts kwenye live (wote wanaona). |
| **Withdraw (host)** | `POST /api/v1/wallet/withdraw` – host anataja kiasi, simu; balance inapungua. Admin: `GET /api/v1/admin/user-withdrawals`, `POST /api/v1/admin/user-withdrawals/{id}/process` (approve/reject; reject = refund). `GET /api/v1/wallet/withdrawals` – historia ya withdrawals. |
| **Backend** | `LiveStreamComment`, `UserCashWithdrawal`, `PeopleYouMayKnowService` (si live), `GiftService` (withdraw + process), `LiveStreamService` (comments). |
| **Migration** | **V4** – `live_stream_comments`, `user_cash_withdrawals`. |

---

## 7. Group (Community) Logic

**Lengo:** Privacy, invite + notification, pin posts, feed order.

| Nini | Mabadiliko |
|------|------------|
| **Create group** | Tayari: name, description, privacy (Public/Private), cover; creator = admin. |
| **Join** | **Public** = mtu anaweza join bila invite. **Private** = join bila invite inakataa (“This group is private. You need an invite.”). |
| **Invite** | Tayari: invite, accept, decline. **Ongezo:** notification inatumwa invitee (“X invited you to join [Group]”) – `NotificationType.COMMUNITY_INVITE`. |
| **Pin post** | Admin anaweza **pin** post: `POST /api/v1/communities/{id}/posts/{postId}/pin`, **unpin**: `DELETE .../pin`. Group feed: **pinned first**, kisha **createdAt DESC**. |
| **Post model** | `Post`: `isPinned`, `pinnedAt`. `PostResponse`: `isPinned`, `pinnedAt`. |
| **Migration** | **V5** – `posts.is_pinned`, `posts.pinned_at`. |

---

## 8. Payment (Haraka Pay) & Payment History

**Lengo:** Thibitisha kununua gift iko na Haraka Pay na kuonyesha historia kwenye profile.

| Nini | Mabadiliko |
|------|------------|
| **Coin purchase** | Tayari: `POST /api/v1/coins/purchase` → `PaymentService.initiatePayment` (Haraka Pay `/api/v1/collect`) → user malipo → job/status inaweka SUCCESS → `processCoinPurchase` → `giftService.purchaseCoins`. |
| **Payment history** | **Ongezo:** `GET /api/v1/payments/me?page=0&size=20` – historia ya malipo ya user (COIN_PURCHASE, SUBSCRIPTION, ORDER, n.k.) kwa **profile**. |
| **Backend** | `PaymentHistoryResponse`, `PaymentService.getMyPayments`, `PaymentController` GET `/me`. |

---

## 9. Migrations (Zote)

| File | Tables / Columns |
|------|------------------|
| **V2** | (Pre-existing) Posts (location, feeling_activity, tagged users), users (profile visibility), user_restrictions, messenger, community, auth_events, n.k. |
| **V3** | `user_contact_hashes` (People You May Know) |
| **V4** | `live_stream_comments`, `user_cash_withdrawals` |
| **V5** | `posts.is_pinned`, `posts.pinned_at` |

---

## 10. API Endpoints – Quick List (Zilizoongezwa / Zilizorekebishwa)

- `POST /auth/register` – register (RegisterRequest: name, email, phone, password + optional currentCity, region, country, dateOfBirth, interests, referralCode)
- `GET /users/me`, `PUT /users/me` – profile; `POST /users/me/avatar`, `POST /users/me/cover`; `GET /users/{id}` – public profile; `GET /users/me/login-activity`
- `POST/DELETE /users/{id}/block`, `GET /users/me/blocked`; `POST/DELETE /users/{id}/restrict`, `GET /users/me/restricted`
- `POST/DELETE /users/{id}/follow`, `GET /users/{id}/followers`, `GET /users/{id}/following`
- `GET /users/suggested` – suggested users (region & country)
- `GET /users/nearby` – people nearby (city → region → country)
- `POST /users/me/contacts` – upload contacts (phones, emails)
- `GET /users/people-you-may-know` – scored “people you may know”
- `GET /posts/feed` – algorithm: recency + engagement + relationship
- `GET /posts/reels` – algorithm: engagement + recency (from following + public)
- `GET /live/.../stories` – story order: closeness + recency
- `POST /live/{id}/comments`, `GET /live/{id}/comments` – live comments
- `GET /gifts/live/{liveStreamId}` – gifts during live
- `POST /wallet/withdraw`, `GET /wallet/withdrawals` – host withdraw gift cash
- `GET /admin/user-withdrawals`, `POST /admin/user-withdrawals/{id}/process` – admin process
- `POST /communities/{id}/posts/{postId}/pin`, `DELETE .../pin` – pin/unpin post
- `GET /payments/me` – payment history (profile)

---

## 11. Flow Summary

1. **Register:** POST /auth/register (name, email, phone, password + optional currentCity, region, country, dateOfBirth, interests, referralCode) → AuthEvent, OTP; data used for discovery.
2. **Profile:** GET/PUT /users/me, avatar, cover, getUserProfile, login-activity; profileVisibility, followingListVisibility; block/restrict, follow/followers/following.
3. **Discovery:** Register/profile (city, region, country, interests) → suggested + nearby + people you may know (contacts + scoring).
4. **Feed:** Posts = recency + engagement + relationship; Stories = closeness + time; Reels = engagement + recency.
5. **Live:** Comment, like, join request, gifts (coins → send → host 70%), host withdraw → admin process.
6. **Groups:** Public = join; Private = invite only; invite → notification; pin post → feed order.
7. **Payments:** Coins (gift) = Haraka Pay; profile = GET /payments/me (historia).

---

*Document iliandikwa kama overview ya mabadiliko kutoka kwenye chat; unaweza kuireference kwenye docs.*
