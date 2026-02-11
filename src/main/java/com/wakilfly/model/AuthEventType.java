package com.wakilfly.model;

/**
 * Type of auth event for background tracking (like Facebook/Instagram).
 */
public enum AuthEventType {
    REGISTRATION,  // User signed up
    LOGIN,         // Successful login
    LOGIN_FAILED,  // Failed login attempt (security/fraud)
    LOGOUT,        // Client-triggered logout (if we track it)
    PASSWORD_RESET_REQUEST,
    PASSWORD_RESET_SUCCESS
}
