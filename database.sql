-- Database Schema for Wakify Project
-- Generated based on JPA Entities in com.wakilfly.model

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    bio TEXT,
    profile_pic VARCHAR(255),
    cover_pic VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    is_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    otp_code VARCHAR(255),
    otp_expires_at TIMESTAMP,
    work VARCHAR(255),
    education VARCHAR(255),
    current_city VARCHAR(255),
    hometown VARCHAR(255),
    relationship_status VARCHAR(50),
    gender VARCHAR(50),
    date_of_birth DATE,
    website VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Follows Join Table (User <-> User)
CREATE TABLE IF NOT EXISTS follows (
    follower_id UUID NOT NULL,
    following_id UUID NOT NULL,
    PRIMARY KEY (follower_id, following_id),
    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE
    FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Friendships Table (Facebook Style)
CREATE TABLE IF NOT EXISTS friendships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id UUID NOT NULL,
    addressee_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL, -- PENDING, ACCEPTED, DECLINED, BLOCKED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (addressee_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(requester_id, addressee_id)
);

-- Communities Table (Groups & Channels)
CREATE TABLE IF NOT EXISTS communities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL DEFAULT 'GROUP', -- GROUP, CHANNEL
    privacy VARCHAR(50) DEFAULT 'PUBLIC', -- PUBLIC, PRIVATE
    cover_image VARCHAR(255),
    creator_id UUID NOT NULL,
    members_count INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Community Members
CREATE TABLE IF NOT EXISTS community_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(50) DEFAULT 'MEMBER', -- ADMIN, MODERATOR, MEMBER
    is_banned BOOLEAN DEFAULT FALSE,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (community_id) REFERENCES communities(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(community_id, user_id)
);

-- Agents Table
CREATE TABLE IF NOT EXISTS agents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    agent_code VARCHAR(255) UNIQUE,
    national_id VARCHAR(255),
    license_number VARCHAR(255),
    id_document_url VARCHAR(255),
    region VARCHAR(255),
    district VARCHAR(255),
    ward VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING',
    is_verified BOOLEAN DEFAULT FALSE,
    total_earnings DECIMAL(12, 2) DEFAULT 0.00,
    available_balance DECIMAL(12, 2) DEFAULT 0.00,
    businesses_activated INTEGER DEFAULT 0,
    total_referrals INTEGER DEFAULT 0,
    approved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Businesses Table
CREATE TABLE IF NOT EXISTS businesses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(255),
    logo VARCHAR(255),
    cover_image VARCHAR(255),
    region VARCHAR(255),
    district VARCHAR(255),
    ward VARCHAR(255),
    street VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    phone VARCHAR(255),
    email VARCHAR(255),
    website VARCHAR(255),
    agent_id UUID,
    status VARCHAR(50) DEFAULT 'PENDING',
    is_verified BOOLEAN DEFAULT FALSE,
    trust_badge VARCHAR(255),
    followers_count INTEGER DEFAULT 0,
    rating DOUBLE PRECISION DEFAULT 0.0,
    reviews_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE SET NULL
);

-- Products Table
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(12, 2) NOT NULL,
    compare_at_price DECIMAL(12, 2),
    stock_quantity INTEGER DEFAULT 0,
    track_stock BOOLEAN DEFAULT TRUE,
    sku VARCHAR(255),
    business_id UUID NOT NULL,
    category VARCHAR(255),
    thumbnail VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    is_featured BOOLEAN DEFAULT FALSE,
    views_count INTEGER DEFAULT 0,
    orders_count INTEGER DEFAULT 0,
    rating DOUBLE PRECISION DEFAULT 0.0,
    reviews_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
);

-- Product Images Table
CREATE TABLE IF NOT EXISTS product_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    url VARCHAR(255) NOT NULL,
    display_order INTEGER DEFAULT 0,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Orders Table
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(255) UNIQUE NOT NULL,
    buyer_id UUID NOT NULL,
    business_id UUID NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    delivery_fee DECIMAL(12, 2) DEFAULT 0.00,
    discount DECIMAL(12, 2) DEFAULT 0.00,
    total DECIMAL(12, 2) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    delivery_address TEXT,
    delivery_phone VARCHAR(255),
    delivery_name VARCHAR(255),
    delivery_region VARCHAR(255),
    delivery_district VARCHAR(255),
    customer_notes TEXT,
    seller_notes TEXT,
    is_paid BOOLEAN DEFAULT FALSE,
    paid_at TIMESTAMP,
    payment_method VARCHAR(255),
    tracking_number VARCHAR(255),
    confirmed_at TIMESTAMP,
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
);

-- Order Items Table
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_image VARCHAR(255),
    unit_price DECIMAL(12, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    total DECIMAL(12, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Subscriptions Table
CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL UNIQUE,
    plan VARCHAR(50) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    auto_renew BOOLEAN DEFAULT FALSE,
    reminder_sent_7_days BOOLEAN DEFAULT FALSE,
    reminder_sent_3_days BOOLEAN DEFAULT FALSE,
    reminder_sent_1_day BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
);

-- Posts Table
CREATE TABLE IF NOT EXISTS posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    caption TEXT,
    author_id UUID NOT NULL,
    visibility VARCHAR(50) DEFAULT 'PUBLIC',
    post_type VARCHAR(50) DEFAULT 'POST',
    shares_count INTEGER DEFAULT 0,
    views_count INTEGER DEFAULT 0,
    original_post_id UUID,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (original_post_id) REFERENCES posts(id) ON DELETE SET NULL
);

-- Post Media Table
CREATE TABLE IF NOT EXISTS post_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL,
    url VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    thumbnail_url VARCHAR(255),
    display_order INTEGER DEFAULT 0,
    duration_seconds INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

-- Comments Table
CREATE TABLE IF NOT EXISTS comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL,
    post_id UUID NOT NULL,
    author_id UUID NOT NULL,
    parent_id UUID,
    likes_count INTEGER DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE
);


-- Post Reactions Table (Replaces Post Likes)
CREATE TABLE IF NOT EXISTS post_reactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL,
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL, -- LIKE, LOVE, HAHA, WOW, SAD, ANGRY
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(post_id, user_id)
);

-- Post Likes Join Table (Deprecated - Use post_reactions)
-- CREATE TABLE IF NOT EXISTS post_likes (
--     post_id UUID NOT NULL,
--     user_id UUID NOT NULL,
--     PRIMARY KEY (post_id, user_id),
--     FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
--     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
-- );

-- Post Product Tags Join Table
CREATE TABLE IF NOT EXISTS post_product_tags (
    post_id UUID NOT NULL,
    product_id UUID NOT NULL,
    PRIMARY KEY (post_id, product_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Conversations Table
CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    participant_one_id UUID NOT NULL,
    participant_two_id UUID NOT NULL,
    last_message_at TIMESTAMP,
    last_message_preview VARCHAR(255),
    is_buyer_seller_chat BOOLEAN DEFAULT FALSE,
    product_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (participant_one_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (participant_two_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Messages Table
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    content TEXT,
    type VARCHAR(50) DEFAULT 'TEXT',
    media_url VARCHAR(255),
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    reference_id UUID,
    reference_type VARCHAR(255),
    actor_id UUID,
    actor_name VARCHAR(255),
    actor_pic VARCHAR(255),
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Payments Table
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    transaction_id VARCHAR(255) UNIQUE,
    external_reference VARCHAR(255),
    amount DECIMAL(12, 2) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    method VARCHAR(50),
    payment_phone VARCHAR(255),
    description VARCHAR(255),
    provider_response TEXT,
    related_entity_type VARCHAR(255),
    related_entity_id UUID,
    paid_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Commissions Table
CREATE TABLE IF NOT EXISTS commissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id UUID NOT NULL,
    business_id UUID,
    amount DECIMAL(12, 2) NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'PENDING',
    paid_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE SET NULL
);

-- Reports Table (Content Moderation)
CREATE TABLE IF NOT EXISTS reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL,
    target_type VARCHAR(50) NOT NULL, -- POST, USER, BUSINESS, PRODUCT, COMMENT
    target_id UUID NOT NULL,
    reason VARCHAR(50) NOT NULL, -- SPAM, HARASSMENT, FALSE_INFO, INAPPROPRIATE, SCAM, OTHER
    description TEXT,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, RESOLVED, DISMISSED
    resolved_by UUID,
    resolution_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (resolved_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Audit Logs Table (Admin Actions)
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID NOT NULL,
    action VARCHAR(100) NOT NULL, -- USER_STATUS_CHANGE, BUSINESS_VERIFIED, AGENT_APPROVED, etc.
    target_type VARCHAR(50), -- USER, BUSINESS, AGENT, WITHDRAWAL, REPORT
    target_id UUID,
    old_value TEXT,
    new_value TEXT,
    details TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Withdrawals Table
CREATE TABLE IF NOT EXISTS withdrawals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id UUID NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    method VARCHAR(50) DEFAULT 'MPESA', -- MPESA, TIGOPESA, AIRTELMONEY, HALOPESA, BANK
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, COMPLETED, FAILED
    transaction_id VARCHAR(255),
    notes TEXT,
    processed_by UUID,
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE,
    FOREIGN KEY (processed_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes for Performance
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_target ON reports(target_type, target_id);
CREATE INDEX idx_audit_logs_admin ON audit_logs(admin_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_withdrawals_agent ON withdrawals(agent_id);
CREATE INDEX idx_withdrawals_status ON withdrawals(status);


-- Update Posts to support communities
ALTER TABLE posts ADD COLUMN community_id UUID;
ALTER TABLE posts ADD CONSTRAINT fk_posts_community FOREIGN KEY (community_id) REFERENCES communities(id) ON DELETE CASCADE;


-- Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL,
    actor_id UUID,
    type VARCHAR(50) NOT NULL,
    entity_id UUID,
    message VARCHAR(255) NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_notifications_recipient ON notifications(recipient_id);


-- Messages Table
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL,
    recipient_id UUID NOT NULL,
    content TEXT NOT NULL,
    media_url VARCHAR(255),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_messages_sender ON messages(sender_id);
CREATE INDEX idx_messages_recipient ON messages(recipient_id);


-- Product Reviews Table
CREATE TABLE IF NOT EXISTS product_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    user_id UUID NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_reviews_product ON product_reviews(product_id);


-- Shopping Cart Tables
CREATE TABLE IF NOT EXISTS carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    UNIQUE(cart_id, product_id)
);

