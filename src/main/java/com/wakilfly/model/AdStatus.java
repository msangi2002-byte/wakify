package com.wakilfly.model;

public enum AdStatus {
    PENDING, // Waiting for approval
    ACTIVE, // Currently running
    PAUSED, // Temporarily paused
    COMPLETED, // Finished running
    REJECTED // Rejected by admin
}
