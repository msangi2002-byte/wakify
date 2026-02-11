package com.wakilfly.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 * Normalize and hash phone/email for contact-based "People You May Know".
 * Same normalization must be used when uploading contacts and when matching User.phone / User.email.
 */
public final class ContactHashUtil {

    private static final Pattern DIGITS = Pattern.compile("\\D+");

    private ContactHashUtil() {}

    /** Normalize phone: digits only (last 9–15 for E.164). */
    public static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return "";
        String digits = DIGITS.matcher(phone).replaceAll("");
        if (digits.length() > 15) digits = digits.substring(digits.length() - 15);
        return digits;
    }

    /** Normalize email: lowercase, trim. */
    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) return "";
        return email.trim().toLowerCase();
    }

    /** SHA-256 hash of value (for storage and matching). */
    public static String hash(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static String hashPhone(String phone) {
        String n = normalizePhone(phone);
        return n.isEmpty() ? "" : hash(n);
    }

    public static String hashEmail(String email) {
        String n = normalizeEmail(email);
        return n.isEmpty() ? "" : hash(n);
    }
}
