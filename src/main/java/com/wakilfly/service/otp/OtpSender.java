package com.wakilfly.service.otp;

/**
 * Interface for sending OTP to users (e.g. via SMS, WhatsApp, Email).
 */
public interface OtpSender {

    /**
     * Send OTP to the given phone number.
     *
     * @param phoneNumber User's phone number (e.g. +255712345678)
     * @param otp         The OTP code to send
     */
    void sendOtp(String phoneNumber, String otp);
}
