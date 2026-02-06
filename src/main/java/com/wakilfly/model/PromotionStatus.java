package com.wakilfly.model;

public enum PromotionStatus {
    PENDING, // Awaiting payment
    ACTIVE, // Currently running
    PAUSED, // Paused by user
    COMPLETED, // Campaign ended
    CANCELLED, // Cancelled before start
    REJECTED // Rejected by admin
}
