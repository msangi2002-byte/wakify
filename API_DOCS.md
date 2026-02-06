# 📚 WAKILFLY API DOCUMENTATION

> **Version**: 1.0.0  
> **Last Updated**: 2026-02-06  
> **Base URL**: `http://localhost:8080/api/v1`

---

## 📋 Table of Contents

1. [Authentication](#1-authentication)
2. [Users & Profiles](#2-users--profiles)
3. [Social (Follow/Unfollow)](#3-social-followunfollow)
4. [Posts](#4-posts)
5. [Likes](#5-likes)
6. [Comments](#6-comments)
7. [Messages](#7-messages)
8. [Business](#8-business)
9. [Products](#9-products)
10. [Orders](#10-orders)
11. [Agent](#11-agent)
12. [Subscriptions](#12-subscriptions)
13. [Payments](#13-payments)
14. [Admin](#14-admin)

---

## 🔐 1. Authentication

### 1.1 Register User
```
POST /api/v1/auth/register
```

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "+255712345678",
  "password": "securePassword123"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "User registered successfully. OTP sent to phone.",
  "data": {
    "userId": "uuid-here",
    "otpRequired": true
  }
}
```

---

### 1.2 Verify OTP
```
POST /api/v1/auth/verify-otp
```

**Request Body:**
```json
{
  "phone": "+255712345678",
  "otp": "123456"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Phone verified successfully",
  "data": {
    "verified": true
  }
}
```

---

### 1.3 Login
```
POST /api/v1/auth/login
```

**Request Body:**
```json
{
  "emailOrPhone": "john@example.com",
  "password": "securePassword123"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "jwt-token-here",
    "refreshToken": "refresh-token-here",
    "user": {
      "id": "uuid",
      "name": "John Doe",
      "email": "john@example.com",
      "phone": "+255712345678",
      "role": "USER",
      "profilePic": "url-to-image"
    }
  }
}
```

---

### 1.4 Refresh Token
```
POST /api/v1/auth/refresh
```

**Request Body:**
```json
{
  "refreshToken": "refresh-token-here"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "new-jwt-token"
  }
}
```

---

### 1.5 Logout
```
POST /api/v1/auth/logout
```

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

---

### 1.6 Forgot Password
```
POST /api/v1/auth/forgot-password
```

**Request Body:**
```json
{
  "phone": "+255712345678"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Password reset OTP sent"
}
```

---

### 1.7 Reset Password
```
POST /api/v1/auth/reset-password
```

**Request Body:**
```json
{
  "phone": "+255712345678",
  "otp": "123456",
  "newPassword": "newSecurePassword123"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Password reset successfully"
}
```

---

## 👤 2. Users & Profiles

### 2.1 Get My Profile
```
GET /api/v1/users/me
```

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+255712345678",
    "bio": "Software Developer",
    "profilePic": "url-to-image",
    "coverPic": "url-to-cover",
    "role": "USER",
    "followersCount": 150,
    "followingCount": 75,
    "postsCount": 42,
    "createdAt": "2026-02-06T10:00:00Z"
  }
}
```

---

### 2.2 Update Profile
```
PUT /api/v1/users/me
```

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "name": "John Updated",
  "bio": "Updated bio here",
  "profilePic": "new-url-or-base64"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Profile updated successfully",
  "data": { ... updated profile ... }
}
```

---

### 2.3 Get User Profile (by ID)
```
GET /api/v1/users/{userId}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Jane Doe",
    "bio": "Designer",
    "profilePic": "url",
    "followersCount": 200,
    "followingCount": 50,
    "postsCount": 30,
    "isFollowing": false,
    "isBusiness": true,
    "businessId": "business-uuid-if-applicable"
  }
}
```

---

### 2.4 Search Users
```
GET /api/v1/users/search?q={query}&page=0&size=20
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      { "id": "uuid", "name": "John", "profilePic": "url", "bio": "..." }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

---

## 🤝 3. Social (Follow/Unfollow)

### 3.1 Follow User
```
POST /api/v1/social/follow/{userId}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "You are now following this user"
}
```

---

### 3.2 Unfollow User
```
DELETE /api/v1/social/follow/{userId}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "You have unfollowed this user"
}
```

---

### 3.3 Get Followers
```
GET /api/v1/social/followers/{userId}?page=0&size=20
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      { "id": "uuid", "name": "Follower Name", "profilePic": "url", "isFollowing": true }
    ],
    "page": 0,
    "totalElements": 150
  }
}
```

---

### 3.4 Get Following
```
GET /api/v1/social/following/{userId}?page=0&size=20
```

---

## 📝 4. Posts

### 4.1 Create Post
```
POST /api/v1/posts
```

**Request Body (multipart/form-data):**
```json
{
  "caption": "Check out this amazing product!",
  "visibility": "PUBLIC",
  "mediaFiles": [ ... files ... ],
  "productTags": ["product-uuid-1", "product-uuid-2"]
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Post created successfully",
  "data": {
    "id": "post-uuid",
    "caption": "Check out this amazing product!",
    "author": { "id": "uuid", "name": "John", "profilePic": "url" },
    "media": [
      { "id": "media-uuid", "url": "url-to-media", "type": "IMAGE" }
    ],
    "productTags": [
      { "id": "product-uuid", "name": "Product Name", "price": 25000, "thumbnail": "url" }
    ],
    "likesCount": 0,
    "commentsCount": 0,
    "createdAt": "2026-02-06T10:00:00Z"
  }
}
```

---

### 4.2 Get Feed
```
GET /api/v1/posts/feed?page=0&size=20
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "post-uuid",
        "caption": "...",
        "author": { ... },
        "media": [ ... ],
        "productTags": [ ... ],
        "likesCount": 50,
        "commentsCount": 10,
        "isLiked": false,
        "createdAt": "..."
      }
    ],
    "page": 0,
    "totalPages": 10
  }
}
```

---

### 4.3 Get Single Post
```
GET /api/v1/posts/{postId}
```

---

### 4.4 Get User Posts
```
GET /api/v1/posts/user/{userId}?page=0&size=20
```

---

### 4.5 Delete Post
```
DELETE /api/v1/posts/{postId}
```

---

## ❤️ 5. Likes

### 5.1 Like Post
```
POST /api/v1/posts/{postId}/like
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Post liked",
  "data": { "likesCount": 51 }
}
```

---

### 5.2 Unlike Post
```
DELETE /api/v1/posts/{postId}/like
```

---

### 5.3 Get Post Likes
```
GET /api/v1/posts/{postId}/likes?page=0&size=20
```

---

## 💬 6. Comments

### 6.1 Add Comment
```
POST /api/v1/posts/{postId}/comments
```

**Request Body:**
```json
{
  "content": "This is amazing!",
  "parentId": null
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": "comment-uuid",
    "content": "This is amazing!",
    "author": { "id": "uuid", "name": "John", "profilePic": "url" },
    "createdAt": "2026-02-06T10:00:00Z"
  }
}
```

---

### 6.2 Get Post Comments
```
GET /api/v1/posts/{postId}/comments?page=0&size=20
```

---

### 6.3 Delete Comment
```
DELETE /api/v1/comments/{commentId}
```

---

## 💌 7. Messages

### 7.1 Get Conversations
```
GET /api/v1/messages/conversations?page=0&size=20
```

---

### 7.2 Get Conversation Messages
```
GET /api/v1/messages/{conversationId}?page=0&size=50
```

---

### 7.3 Send Message
```
POST /api/v1/messages
```

**Request Body:**
```json
{
  "recipientId": "user-uuid",
  "content": "Hello!",
  "type": "TEXT"
}
```

---

## 🏪 8. Business

*Coming in Phase 2...*

---

## 📦 9. Products

*Coming in Phase 2...*

---

## 🛒 10. Orders

*Coming in Phase 3...*

---

## 🧑‍💼 11. Agent

*Coming in Phase 4...*

---

## 💳 12. Subscriptions

*Coming in Phase 4...*

---

## 💰 13. Payments

*Coming in Phase 4...*

---

## 🛡️ 14. Admin

*Coming in Phase 4...*

---

## 🔔 Callbacks & Webhooks

### Payment Callback (Mobile Money)
```
POST /api/v1/webhooks/payment
```

**Request Body (from payment provider):**
```json
{
  "transactionId": "txn-123",
  "status": "SUCCESS",
  "amount": 20000,
  "phone": "+255712345678",
  "reference": "SUB-uuid"
}
```

---

## 📊 Response Format (Standard)

All API responses follow this format:

**Success:**
```json
{
  "success": true,
  "message": "Operation message",
  "data": { ... }
}
```

**Error:**
```json
{
  "success": false,
  "message": "Error description",
  "errors": [
    { "field": "email", "message": "Email is required" }
  ]
}
```

---

## 🔒 Authentication

All protected endpoints require:
```
Authorization: Bearer {accessToken}
```

---

## 📌 Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK - Success |
| 201 | Created - Resource created |
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Token invalid/missing |
| 403 | Forbidden - No permission |
| 404 | Not Found - Resource not found |
| 500 | Server Error |

---

> 📝 **Note**: This document is updated as new APIs are added.
