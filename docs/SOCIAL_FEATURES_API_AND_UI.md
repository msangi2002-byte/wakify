# Wakify – Social Features: API & UI/UX Guide

Hati hii inaelezea **API za backend** zilizotumika na **maelekezo ya UI/UX** kwa vipengele vilivyoongezwa: Save post, Share (repost + share to story), Hashtags, Who viewed my story, Block/Report.

---

## 1. Save Post (Hifadhi)

### Backend (tayari iko)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/posts/{postId}/save` | Save post to "Saved" list (auth required) |
| DELETE | `/api/v1/posts/{postId}/save` | Remove from Saved (auth required) |
| GET | `/api/v1/posts/saved?page=0&size=20` | List current user's saved posts (auth required) |

- **PostResponse** ina field `saved: boolean` – true ikiwa mtumiaji aliye-login amehifadhi post hiyo.

### UI/UX

- **Kwenye post card:** Onesha kitufe **Save** (icon ya bookmark / hifadhi). Ikiwa `saved === true`, onesha state “saved” (k.g. bookmark filled).
- **Profile:** Tab au sehemu **“Saved”** (Hifadhi) – wito `GET /posts/saved`; onesha grid au list ya posts zilizohifadhiwa.
- **Empty state:** “Hakuna post zilizohifadhiwa” / “No saved posts”.

---

## 2. Share Post (Repost + Share to Story)

### Backend (tayari iko)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/posts` | Create post. Body: `{ "caption": "...", "originalPostId": "<uuid>", "postType": "POST" }` → **Repost to feed**. |
| POST | `/api/v1/posts/{postId}/share-to-story` | Share post as story (24h). Body optional: `{ "caption": "..." }`. |

- **PostResponse** ina `originalPost: PostResponse` ikiwa ni repost/shared post.

### UI/UX

- **Kwenye post card:** Kitufe **Share** (icon share).
  - **Repost to feed:** Dialog “Repost?” → POST create post na `originalPostId` + `postType: POST`.
  - **Share to story:** “Share to story” → POST `/posts/{postId}/share-to-story` (na caption optional).
- **Stories:** Story inayotokana na “Share to story” inaweza kuonyesha original post (caption/media) kama card ndani ya story.

---

## 3. Hashtags (#)

### Backend (tayari iko)

- **Caption:** Unapounda post na caption yenye `#mzumbe` au `#darasalaam`, backend ina-parse na ku-link hashtags kwa post.
- **PostResponse** ina `hashtags: string[]` – orodha ya majina ya hashtag (bila #).

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/hashtags/trending?page=0&size=20` | Trending hashtags (Explore) |
| GET | `/api/v1/hashtags/{tagName}/posts?page=0&size=20` | Posts by hashtag (tagName with or without #) |

### UI/UX

- **Create post:** Caption inaweza kuwa na `#tag`. Onesha tags kwenye preview; link tag → search/Explore by that tag.
- **Explore tab:** Sehemu “Trending hashtags” – wito `GET /hashtags/trending`; kila tag inaweza kuwa link → `/explore/hashtag/mzumbe` (au route yako).
- **Explore by tag:** Ukurasa “#mzumbe” – wito `GET /hashtags/mzumbe/posts`; onesha grid/list ya posts.
- **Post card:** Chini ya caption onesha `hashtags` kama links (click → Explore by that hashtag).

---

## 4. Who Viewed My Story

### Backend (tayari iko)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/posts/{postId}/view` | Record that current user viewed this story (auth). Inaruhusiwa kwa postType = STORY tu. |
| GET | `/api/v1/posts/{postId}/viewers?page=0&size=50` | List viewers (auth; **ni story author tu**). Response: PagedResponse of UserSummary (id, name, profilePic). |

### UI/UX

- **Wakati mtumiaji anatazama story:** Mwisho wa story au wakati anaanza kuona, piga **POST /posts/{postId}/view** (moja kwa story; unaweza kudebounce).
- **Story author:** Kitufe/icon “Viewers” au “X watu wameona” → bottom sheet au modal yenye orodha ya watu (avatar, name) kutoka `GET /posts/{postId}/viewers`.
- **Empty:** “Hakuna mtu ameona bado” ikiwa list tupu.

---

## 5. Block User & Report

### Backend (tayari iko)

**Block**

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/users/{userId}/block` | Block user (auth) |
| DELETE | `/api/v1/users/{userId}/block` | Unblock (auth) |
| GET | `/api/v1/users/me/blocked?page=0&size=20` | List blocked users (auth) |

- Feed na stories **tayari zinaachwa nje** watu walio-block / walioblock (backend).
- Kuona post ya mtu aliyeblock / uliyemblock: API inarudisha 404 / empty kwa consistency.

**Report**

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/reports` | Body: `{ "type": "USER"|"POST"|"COMMENT"|..., "targetId": "<uuid>", "reason": "...", "description": "..." }`. |

### UI/UX

- **Profile / post menu:** “Block user” → POST `/users/{userId}/block`. Baada ya block, onesha “User blocked” na usionyeshe content yao tena.
- **Settings / Account:** “Blocked users” → GET `/users/me/blocked`; orodha na kitufe “Unblock” (DELETE).
- **Report:** “Report post” / “Report user” → form (reason, description) → POST `/reports` na `type` + `targetId`. Onesha “Report submitted” message.

---

## 6. Mfumo wa Lugha (Swahili / English) na Dark Mode

- **Lugha:** Hizi API hazibadilishi lugha; labels na messages zinakuja kutoka frontend. Onesha labels kwa lugha iliyochaguliwa (k.g. “Hifadhi” / “Save”, “Wameona” / “Viewers”).
- **Dark mode:** Toggle theme kwenye frontend; CSS variables / Tailwind dark: prefix. Backend haitumi theme.

---

## 7. Orodha ya Vipengele Vilivyotumika (Backend)

| Kipengele | Status | API / Entity |
|-----------|--------|--------------|
| Save post | ✅ | SavedPost, POST/DELETE save, GET saved |
| Share to feed (repost) | ✅ | Create post with originalPostId |
| Share to story | ✅ | POST /posts/{id}/share-to-story |
| Hashtags | ✅ | Hashtag, post_hashtags, parse caption, GET trending & by tag |
| Who viewed my story | ✅ | StoryView, POST view, GET viewers |
| Block user | ✅ | UserBlock, block/unblock, feed & stories filtered |
| Report | ✅ | Report (POST /reports) – tayari ilikuwepo |

---

## 8. Zingine (Mentions, Drafts, Highlights)

- **Mentions (@user):** Kwa sasa haijaimplement kwenye backend; unaweza kuongeza parsing ya @username kwenye caption/comments na notification type MENTION.
- **Drafts:** Unaweza kuongeza `isDraft` kwenye Post na endpoints “save draft” / “publish”.
- **Stories highlights:** Unaweza kuongeza entity “StoryHighlight” (user, title, story post IDs) na API ya kuunda/kuondoa highlights.

Kama unahitaji maelezo zaidi kwa endpoint fulani au UI flow, taja kipengele na ukurasa (k.m. Profile, Explore, Post card).
