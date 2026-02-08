package com.wakilfly.service.otp;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Sends OTP via email using SMTP.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailOtpSender {

    @Value("${spring.mail.username:otp@wakilfy.com}")
    private String fromEmail;

    private final JavaMailSender mailSender;

    /**
     * Send OTP to the given email address.
     */
    public void sendOtp(String email, String otp) {
        if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
            log.warn("Cannot send OTP email: email or otp is empty");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(email.trim());
            helper.setSubject("Wakify OTP - Msimbo wako wa uthibitishaji");
            helper.setText(buildEmailBody(otp), true);

            mailSender.send(message);
            log.info("OTP sent via email to {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", email, e.getMessage());
        }
    }

    private String buildEmailBody(String otp) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 400px; margin: 0 auto;">
                <h2 style="color: #333;">Wakify OTP</h2>
                <p>Msimbo wako wa uthibitishaji ni:</p>
                <p style="font-size: 24px; font-weight: bold; color: #0066cc; letter-spacing: 4px;">%s</p>
                <p>Usichanganye msimbo huu na mtu mwingine. Inakwisha ndani ya dakika 10.</p>
                <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                <p style="color: #888; font-size: 12px;">Wakify - Huu ni barua pepe ya kiotomatiki.</p>
            </div>
            """.formatted(otp);
    }
}
