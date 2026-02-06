package com.wakilfly.entity;

public enum BusinessStatus {
    PENDING, // Waiting for agent activation
    ACTIVE, // Active and operational
    SUSPENDED, // Temporarily suspended
    EXPIRED, // Subscription expired
    INACTIVE // Deactivated
}
