# WAKIFY ADMIN PANEL – FEATURES SPECIFICATION (COMPLETE)

**Version:** 1.0  
**Date:** February 2025  
**Purpose:** Comprehensive specification for Admin & Sub-Admin panel – scalable to global level (Facebook/Instagram scale)

---

## TABLE OF CONTENTS

1. [Dashboard & Overview](#1-dashboard--overview)
2. [Map & Geo Features](#2-map--geo-features)
3. [User Management](#3-user-management)
4. [Content & Media Moderation](#4-content--media-moderation)
5. [Media Detection & Analytics](#5-media-detection--analytics)
6. [Business & Agent Management](#6-business--agent-management)
7. [Payment & Transaction Reports](#7-payment--transaction-reports)
8. [Financial Oversight](#8-financial-oversight)
9. [Reports & Moderation](#9-reports--moderation)
10. [Admin & Sub-Admin Roles](#10-admin--sub-admin-roles)
11. [Trust & Safety](#11-trust--safety)
12. [Analytics & Monitoring](#12-analytics--monitoring)
13. [System Config & Operations](#13-system-config--operations)
14. [Notifications & Alerts](#14-notifications--alerts)
15. [Compliance & Legal](#15-compliance--legal)
16. [Additional Features](#16-additional-features)

---

## 1. DASHBOARD & OVERVIEW

### 1.1 Quick Stats Cards
- Total users (all time)
- Active users (today / 7 days / 30 days)
- New registrations (today / week / month)
- Total businesses
- Active vs pending businesses
- Total agents
- Active vs pending agents
- Total orders
- Orders by status (pending, completed, cancelled, refunded)
- Total revenue (all time)
- Revenue (today, week, month)
- Total posts (all content)
- Total products
- Active promotions
- Pending reports (action required)
- Pending withdrawals
- Active subscriptions
- Expiring subscriptions (next 7 days)

### 1.2 Charts & Visualizations
- User signups trend (line chart – 7/30/90 days)
- Revenue trend (line chart – daily/weekly/monthly)
- Users by role (pie chart)
- Orders by status (bar chart)
- Revenue by type (pie: subscription, orders, agent fees, promotions)
- Top regions by activity (bar chart)
- Peak hours heatmap (when users most active)

### 1.3 Alerts & Action Items
- Reports awaiting review (count + link)
- Withdrawals pending approval (count + link)
- Agent registrations pending (count + link)
- Failed payments (retry needed)
- Content flagged by AI (review queue)
- User appeals pending
- System health warnings (if any)

### 1.4 Recent Activity Feed
- Last 20 admin actions (who did what, when)
- Last 10 new user registrations
- Last 10 orders
- Last 5 reported items

---

## 2. MAP & GEO FEATURES

### 2.1 User Map
- Plot all users on world map (by last known location / registration region)
- Filter: by role, active/inactive, date range
- Cluster view when zoomed out
- Click marker → user summary popup
- Toggle: show online users only
- Export: users in selected region

### 2.2 Business Map
- All businesses plotted by location (lat/lng or region)
- Filter: category, status, verified
- Cluster by city/region
- Click → business details popup
- Show delivery zones overlay
- Density heatmap by region

### 2.3 Content Map
- Geo-tagged posts/media on map
- Filter: post type, date, reported/clean
- Identify content hotspots
- Remove content from map view (moderation)

### 2.4 Agent Coverage Map
- Agents plotted by region
- Show coverage gaps (areas without agents)
- Agent territory boundaries
- Performance by region (activations, commissions)

### 2.5 Heatmaps
- User activity heatmap (where users most active)
- Order/delivery heatmap
- Content creation heatmap
- Revenue heatmap by region

### 2.6 Region Dashboard
- Stats per region: Dar, Mwanza, Arusha, Mtwara, etc.
- Compare regions (users, revenue, orders)
- Drill-down to district/ward level
- Export region report

---

## 3. USER MANAGEMENT

### 3.1 User List
- Paginated table (20/50/100 per page)
- Columns: ID, name, phone, email, role, verified, active, joined, last seen
- Sort by: name, date, role, activity
- Export: CSV, Excel

### 3.2 Search & Filters
- Search by: name, phone, email, user ID
- Filter by: role (USER, BUSINESS, AGENT, ADMIN), isActive, isVerified
- Filter by: date joined (range)
- Filter by: region/country
- Filter by: has business / no business
- Filter by: referred by agent

### 3.3 User Detail View
- Full profile info
- Activity timeline (posts, orders, logins)
- Associated business (if any)
- Associated agent record (if any)
- Followers/following count
- Login history (IP, device, location, timestamp)
- Report history (reported / filed reports)
- Payment history
- Audit log for this user

### 3.4 Quick Actions
- Activate / Deactivate account
- Verify (blue tick)
- Change role
- Ban (temporary / permanent, with reason)
- Send warning message
- Reset password
- Impersonate (login as user – for support, logged)

### 3.5 Bulk Actions
- Select multiple users → Activate, Deactivate, Export
- Bulk role change (with approval)
- Bulk export selected users

### 3.6 User Intelligence
- Risk score (0–100 based on reports, violations)
- Device fingerprint (detect multi-accounting)
- IP history (duplicate account detection)
- Activity score (engagement level)

---

## 4. CONTENT & MEDIA MODERATION

### 4.1 Moderation Queue
- All content awaiting review (posts, reels, stories, comments)
- Priority: reported first, then AI-flagged
- Filter: type (post, comment, product), status, date
- Preview content inline
- Actions: Remove, Allow, Warn user, Ban user
- Bulk approve / bulk remove

### 4.2 Reports Queue
- All user reports (posts, users, businesses, comments, products)
- Filter: report type, status (pending, resolved, dismissed)
- Assign to moderator
- Resolution: Remove content, Warn, Ban, Dismiss
- Add resolution notes
- Escalate to senior admin

### 4.3 Content Takedown
- Remove post/video/comment
- Reason (required): policy violation, copyright, etc.
- Notify user
- Log for appeal

### 4.4 Blocklist Management
- Keyword blocklist (auto-flag posts)
- Hashtag blocklist (block/restrict)
- Domain blocklist (spam links)
- Add, edit, remove entries
- Test blocklist (preview matches)

### 4.5 Live Stream Monitor
- List active live streams
- Watch stream in panel
- End stream immediately (violation)
- Warn streamer
- Ban from live

### 4.6 DM/Chat Moderation
- Search reported conversations
- View message thread (with legal/compliance approval)
- Take action: warn, ban, delete messages

---

## 5. MEDIA DETECTION & ANALYTICS

### 5.1 Automated Detection (AI/ML)
- **NSFW/Nudity Detection** – flag inappropriate images/videos
- **Hate Speech Detection** – text in posts, comments
- **Violence/Gore Detection** – violent content
- **Copyright Detection** – music, video clips
- **Deepfake Detection** – synthetic media
- **Spam/Phishing Detection** – malicious links
- **Duplicate Content** – reposts, copies
- **Fake Profile Detection** – stolen photos, celebrity pics
- **Bullying/Harassment Detection** – text patterns
- **Self-Harm Detection** – sensitive content

### 5.2 Media Review Queue (AI-Flagged)
- Content auto-flagged by AI
- Confidence score per flag
- Human review: Confirm / False positive
- Feedback loop to improve AI

### 5.3 Media Statistics
- **Total media count**: images, videos, reels, stories (global)
- **Per user**: media count, storage used
- **Storage usage**: total GB, per type, trends
- **Format breakdown**: JPEG, PNG, MP4, etc.
- **Upload trends**: uploads per day/week/month
- **Largest files**: identify abuse (huge uploads)
- **Failed uploads**: count, retry option

### 5.4 Content Analytics
- Most viewed posts
- Most reported content types
- Removal rate by category
- Average time to moderation

---

## 6. BUSINESS & AGENT MANAGEMENT

### 6.1 Business List
- All businesses, paginated
- Filter: status, category, region, verified
- Search by name, owner
- Actions: Verify, Activate, Suspend
- Bulk verify

### 6.2 Business Detail
- Full business info
- Owner details
- Agent who activated
- Products count
- Orders count
- Revenue
- Subscription status
- Verification documents (if any)

### 6.3 Agent List
- All agents, paginated
- Filter: status (PENDING, ACTIVE, SUSPENDED)
- Search by name, code, phone
- Actions: Approve, Suspend, Verify

### 6.4 Agent Detail
- Profile, code, region
- Businesses activated
- Commissions earned
- Withdrawals
- Performance metrics

---

## 7. PAYMENT & TRANSACTION REPORTS

### 7.1 Transaction List (All Payments)
- **Columns**: ID, transaction ID, user, amount, type, status, method, date, description
- **Pagination**: 20/50/100 per page
- **Export**: CSV, Excel (filtered results)

### 7.2 Transaction Filters
- **Status**: PENDING, SUCCESS, FAILED, REFUNDED, CANCELLED
- **Type**: AGENT_REGISTRATION, BUSINESS_ACTIVATION, SUBSCRIPTION, ORDER, COIN_PURCHASE, AGENT_PACKAGE, PROMOTION
- **Date range**: from – to
- **User**: by user ID or search
- **Amount range**: min – max
- **Payment method**: M-Pesa, Tigo Pesa, Airtel Money
- **Transaction ID**: exact search

### 7.3 Transaction Detail
- Full payment record
- User who paid
- Related entity (order, business request, etc.)
- Provider response (HarakaPay)
- Timeline: created, paid, refunded

### 7.4 Transaction Reports (Pre-built)
| Report | Description | Export |
|--------|-------------|--------|
| **Daily Revenue Report** | Revenue by day, by type | CSV, PDF |
| **Weekly Revenue Report** | Summary by week | CSV, PDF |
| **Monthly Revenue Report** | Full monthly breakdown | CSV, PDF |
| **Revenue by Type** | Agent reg, business act, orders, etc. | CSV |
| **Revenue by Region** | Geographic breakdown | CSV |
| **Failed Payments Report** | All failed, reason, retry status | CSV |
| **Pending Payments Report** | Stuck pending (> 24h) | CSV |
| **Refund Report** | All refunds, dates, amounts | CSV |
| **Agent Commission Report** | Commissions paid per agent | CSV |
| **Subscription Revenue** | Recurring revenue | CSV |
| **Top Paying Users** | Users by total spent | CSV |
| **Payment Method Breakdown** | M-Pesa vs Tigo vs Airtel | Chart + CSV |

### 7.5 Transaction Analytics
- Revenue chart (daily/weekly/monthly)
- Success vs failure rate
- Average transaction value
- Peak payment hours
- Payment method distribution
- Refund rate trend

### 7.6 Reconcile with Provider
- Export transactions for date range
- Compare with HarakaPay/payment provider statement
- Flag mismatches
- Settlement report

### 7.7 Refund Management
- Initiate refund (full/partial)
- Refund queue (requested by user)
- Refund history
- Refund reason tracking

---

## 8. FINANCIAL OVERSIGHT

### 8.1 Revenue Dashboard
- Total revenue (all time)
- Revenue by period (day, week, month, quarter, year)
- Revenue by type (chart + table)
- Revenue by region
- Revenue growth rate
- Projected revenue (simple forecast)

### 8.2 Withdrawal Management
- Pending withdrawals list
- Agent details, amount, payment info
- Approve (with transaction ID) / Reject (with reason)
- Bulk approve (batch processing)
- Withdrawal history
- Withdrawal by agent report

### 8.3 User Cash Withdrawals (Gifts → Pesa)
- Pending user withdrawals (gift earnings)
- Approve / Reject
- Transaction ID recording

### 8.4 Fraud Detection
- Flag suspicious patterns: multiple failed cards, chargebacks
- Unusual transaction volume per user
- Duplicate transaction detection
- High-risk user list
- Manual review queue for flagged transactions

### 8.5 Fee Configuration
- Agent registration fee (editable)
- Business activation fee (editable)
- Commission rates
- Subscription plan prices
- Promotion/ad pricing

### 8.6 Settlement Reports
- Daily settlement summary
- Provider payout status
- Platform commission/fees earned
- Net revenue after payouts

---

## 9. REPORTS & MODERATION

### 9.1 Report Center
- All reports (posts, users, businesses, products, comments)
- Filter: type, status, date, reporter
- Priority sorting (critical first)
- Assign to moderator
- Resolution workflow

### 9.2 Report Types
- Post (inappropriate, spam, other)
- User (harassment, impersonation, spam)
- Business (fake, scam)
- Product (counterfeit, misleading)
- Comment (offensive, spam)

### 9.3 Appeal Center
- User appeals (against ban, content removal)
- Filter: status (pending, approved, rejected)
- Review appeal
- Approve (restore) / Reject (with reason)
- Communication with user

### 9.4 Ban Management
- List all banned users
- Ban type: temporary (with expiry) / permanent
- Ban reason (required)
- Unban (with approval)
- Ban history per user

### 9.5 Shadow Ban
- Limit user reach without notifying
- Toggle on/off
- Track shadow-banned users

---

## 10. ADMIN & SUB-ADMIN ROLES

### 10.1 Role Types
| Role | Scope | Key Permissions |
|------|-------|-----------------|
| **Super Admin** | Full | Everything, including admin management |
| **Admin** | Full | All except create/delete admins |
| **Moderation Admin** | Content | Reports, content queue, user ban |
| **Support Admin** | Users | View users, orders, no delete |
| **Finance Admin** | Money | Payments, withdrawals, revenue |
| **Region Admin** | Single region | Users/businesses in assigned region only |
| **Analytics Admin** | Read-only | Dashboards, reports, no edit |

### 10.2 Granular Permissions (Checkboxes)
- **Users**: view, search, edit, activate/deactivate, ban, delete, verify, change_role, impersonate
- **Content**: view, remove, feature, pin, review_queue, blocklist_edit
- **Business**: view, verify, suspend, delete
- **Agents**: view, approve, suspend, verify
- **Reports**: view, resolve, dismiss, escalate
- **Financial**: view_payments, approve_withdrawal, refund, view_revenue
- **Settings**: view, edit_fees, edit_packages, feature_flags
- **Audit**: view_logs
- **Map**: view
- **Analytics**: view_dashboard
- **Admins**: create, edit, delete (Super Admin only)

### 10.3 Sub-Admin Management
- Create sub-admin (email, name, role)
- Assign permissions (granular)
- Assign region (if Region Admin)
- Activate / Deactivate sub-admin
- View sub-admin activity
- Force logout / revoke session

### 10.4 Admin Security
- 2FA for all admin accounts
- Session management (view active sessions)
- IP allowlist (optional)
- Audit: every admin action logged

---

## 11. TRUST & SAFETY

### 11.1 User Risk Score
- Algorithm: reports + violations + patterns
- Display 0–100 score
- High-risk users flagged
- Action: monitor, restrict, ban

### 11.2 Rate Limit Override
- Increase/decrease API limits per user
- Temporary boost for VIP
- Throttle abusive user

### 11.3 Whitelist / Blacklist
- IP blacklist (block access)
- Phone blacklist (block registration)
- Email blacklist
- Whitelist (bypass certain checks)

### 11.4 Crisis Mode
- Emergency toggle: pause new uploads
- Pause live streams
- Read-only mode for users
- Communication banner to all users

### 11.5 Legal Hold
- Preserve user data for legal request
- Freeze account (no delete)
- Export for legal team

---

## 12. ANALYTICS & MONITORING

### 12.1 Real-time Stats
- DAU (daily active users)
- MAU (monthly active users)
- Posts per minute
- Orders per minute
- Revenue per hour
- Active live streams count

### 12.2 Growth Metrics
- User growth (chart)
- Business growth
- Agent growth
- Retention (D1, D7, D30)
- Cohort analysis

### 12.3 Engagement Metrics
- Likes, comments, shares (totals, trends)
- Average session duration
- Posts per user
- Follow rate
- Conversion: signup → first order

### 12.4 Funnel Analysis
- Signup → OTP verify → First post
- Signup → Business request → Payment → Active business
- User → Agent registration → Payment → Active agent

### 12.5 Performance Monitoring
- API latency (p50, p95, p99)
- Error rate
- Uptime %
- Slow query log
- Database connection pool

### 12.6 SLA Dashboard
- Uptime by service
- Response time trends
- Incident history

---

## 13. SYSTEM CONFIG & OPERATIONS

### 13.1 Feature Flags
- Enable/disable features globally
- Per region rollout
- Per user segment (beta testers)
- Scheduled activation

### 13.2 Maintenance Mode
- Toggle: app in maintenance
- Custom message to users
- Allow admin access during maintenance

### 13.3 Rate Limits
- Global API rate limit
- Per-endpoint limits
- Per-user overrides
- Throttle config

### 13.4 Cache Control
- Clear cache (all or selective)
- Warm cache for popular content
- Cache stats (hit rate)

### 13.5 Queue Monitor
- Job queues: emails, notifications, exports
- Failed jobs (retry, view error)
- Queue depth
- Processing rate

### 13.6 Database Stats
- Table sizes
- Slow queries
- Index usage
- Connection count

### 13.7 API Usage
- Requests per endpoint
- Top consumers (by user)
- Rate limit hits
- Error breakdown

---

## 14. NOTIFICATIONS & ALERTS

### 14.1 Alert Rules (Configurable)
- Spike in reports (> X in 1 hour)
- Failed payments (> X% in 1 hour)
- Revenue drop (> X% day-over-day)
- New user spike (potential bot)
- API error rate > threshold
- Pending withdrawals > X
- Content queue backlog > X

### 14.2 Alert Channels
- In-app notification (admin panel)
- Email to admin(s)
- Slack/Teams webhook
- SMS (critical only)

### 14.3 Daily Digest
- Summary email: new users, revenue, reports, pending actions
- Configurable: which admins receive

### 14.4 Escalation
- Auto-escalate: report not resolved in 24h → senior admin
- Critical: multiple reports same content → immediate alert

---

## 15. COMPLIANCE & LEGAL

### 15.1 Data Export (GDPR / CCPA)
- User requests data export
- Admin generates export (profile, posts, orders, etc.)
- Download link (expiring)
- Log export requests

### 15.2 Account Deletion
- User requests deletion
- Admin processes: full delete or anonymize
- Retention period config
- Deletion log

### 15.3 Content Takedown Requests
- Log: DMCA, court order, government request
- Status: received, processing, completed
- Content removed
- Response to requester

### 15.4 Audit Export
- Export audit logs (date range)
- For compliance review
- Immutable format

### 15.5 Terms & Policy Versions
- Track T&C, Privacy Policy versions
- User consent log (which version accepted)
- Notify users of updates

---

## 16. ADDITIONAL FEATURES

### 16.1 Orders Management (Admin View)
- All orders across platform
- Filter: status, business, date
- Override: cancel, mark delivered
- Order detail (items, customer, delivery)
- Refund order
- Dispute resolution

### 16.2 Subscriptions Management
- All active subscriptions
- Cancel, extend, change plan
- Grace period config
- Expiry reminders
- Churn report

### 16.3 Promotions & Ads
- All active promotions
- Pause, cancel, extend
- Ad spend tracking
- Performance by promotion
- Boost post approvals (if manual)

### 16.4 Notifications (System-wide)
- Send in-app notification to all users
- Segment: by role, region, last active
- Scheduled send
- A/B test messages

### 16.5 Announcements / Banners
- Create banner for app (maintenance, holiday)
- Schedule start/end
- Target audience
- Dismissible / persistent

### 16.6 Support Tickets (Optional)
- If integrated: view support tickets
- Assign to agent
- Resolution status
- Escalate

### 16.7 Referral Program Admin
- Referral stats (agents, codes used)
- Top referrers
- Referral fraud detection
- Bonus payouts

### 16.8 Community Management
- List all communities
- Suspend community
- Remove admin/moderator
- Merge communities
- View community stats

### 16.9 Product Catalog Oversight
- Products pending approval (if workflow exists)
- Flag inappropriate products
- Bulk deactivate
- Category management
- Product report queue

### 16.10 Review Moderation
- Product reviews reported
- Business reviews reported
- Remove review
- Reply as business (support)

### 16.11 Invite / Referral Codes
- System-wide invite codes
- Agent codes performance
- Generate promo codes
- Usage tracking

### 16.12 A/B Testing (Admin)
- Create experiment (feature, percentage)
- View results
- Declare winner
- Ramp to 100%

### 16.13 Backup & Restore
- Trigger manual backup
- Restore from backup (disaster recovery)
- Backup schedule config
- Retention policy

### 16.14 Log Viewer
- Application logs (searchable)
- Error logs
- Security logs (login attempts, failures)
- Filter by level, date, user

### 16.15 System Health
- Service status (API, DB, Redis, S3)
- Disk space
- Memory usage
- CPU usage
- External API status (HarakaPay, etc.)

### 16.16 Help / Onboarding
- Admin panel user guide
- Video tutorials
- FAQ
- Contact dev team
- Changelog (new features)

---

## SUMMARY: FEATURE CHECKLIST

| Category | Count | Priority |
|----------|-------|----------|
| Dashboard | 20+ | P0 |
| Map & Geo | 15+ | P1 |
| User Management | 25+ | P0 |
| Content Moderation | 20+ | P0 |
| Media Detection | 15+ | P0 |
| Business/Agent | 15+ | P0 |
| **Payment & Transactions** | **25+** | **P0** |
| Financial | 20+ | P0 |
| Reports | 15+ | P0 |
| Admin Roles | 15+ | P0 |
| Trust & Safety | 12+ | P1 |
| Analytics | 20+ | P1 |
| System Config | 15+ | P1 |
| Notifications | 10+ | P1 |
| Compliance | 10+ | P2 |
| Additional | 35+ | P1-P3 |

**Total: 280+ features**

---

## IMPLEMENTATION PRIORITY

### Phase 1 (MVP Admin)
- Dashboard stats, Payment/Transaction reports, User management, Reports queue, Basic sub-admin

### Phase 2 (Scale)
- Map, Media detection, Granular permissions, Analytics, Alerts

### Phase 3 (Enterprise)
- Compliance, Advanced fraud, A/B testing, Full audit, Crisis mode

---

*Document end. Wakify Admin Panel – Built for global scale.*
