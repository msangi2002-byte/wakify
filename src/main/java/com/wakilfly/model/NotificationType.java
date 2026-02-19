package com.wakilfly.model;

public enum NotificationType {
    LIKE,
    COMMENT,
    SHARE,
    FRIEND_REQUEST,
    FRIEND_ACCEPT,
    FOLLOW,
    COMMUNITY_INVITE, // Invited to join a group
    BUSINESS_REQUEST_RECEIVED, // User requested to become a business (agent gets notified)
    PROMOTION_APPROVED,
    PROMOTION_PAUSED,
    PROMOTION_REJECTED,
    SYSTEM
}
