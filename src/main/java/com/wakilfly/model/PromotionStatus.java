package com.wakilfly.model;

public enum PromotionStatus {
    PENDING, // Awaiting payment
    PENDING_APPROVAL, // Paid, awaiting admin policy/approval before going live
    ACTIVE, // Currently running
    PAUSED, // Paused by user
    COMPLETED, // Campaign ended
    CANCELLED, // Cancelled before start
    REJECTED // Rejected by admin
}
