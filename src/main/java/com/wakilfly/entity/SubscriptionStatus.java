package com.wakilfly.entity;

public enum SubscriptionStatus {
    PENDING, // Waiting for payment
    ACTIVE, // Active subscription
    GRACE, // Grace period (limited features)
    EXPIRED, // Subscription expired
    CANCELLED // Cancelled by user/admin
}
