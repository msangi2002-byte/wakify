package com.wakilfly.entity;

public enum NotificationType {
    FOLLOW, // Someone followed you
    LIKE, // Someone liked your post
    COMMENT, // Someone commented on your post
    MENTION, // Someone mentioned you
    MESSAGE, // New message
    ORDER, // Order update
    SUBSCRIPTION, // Subscription reminder/expiry
    BUSINESS_ACTIVATED, // Business account activated
    COMMISSION, // Commission earned (for agents)
    SYSTEM // System notification
}
