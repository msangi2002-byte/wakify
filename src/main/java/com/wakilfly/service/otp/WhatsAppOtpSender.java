package com.wakilfly.service.otp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Sends OTP via W-OTP service (WhatsApp using Baileys).
 * Calls POST {wotp.service-url}/send-otp with phoneNumber and otp.
 */
@Component
@Slf4j
public class WhatsAppOtpSender implements OtpSender {

    @Value("${wotp.service-url:}")
    private String wotpServiceUrl;

    @Value("${wotp.api-key:}")
    private String wotpApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void sendOtp(String phoneNumber, String otp) {
        if (phoneNumber == null || phoneNumber.isBlank() || otp == null || otp.isBlank()) {
            log.warn("Cannot send OTP: phone or otp is empty");
            return;
        }

        if (wotpServiceUrl == null || wotpServiceUrl.isBlank()) {
            log.info("W-OTP URL not configured. OTP for {}: {} (log only)", phoneNumber, otp);
            return;
        }

        String url = wotpServiceUrl.replaceAll("/$", "") + "/send-otp";
        Map<String, String> body = Map.of(
                "phoneNumber", phoneNumber.trim(),
                "otp", otp
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (wotpApiKey != null && !wotpApiKey.isBlank()) {
                headers.set("X-API-Key", wotpApiKey);
            }
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("OTP sent via WhatsApp to {}", phoneNumber);
            } else {
                log.warn("W-OTP returned non-2xx for {}: {}", phoneNumber, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send OTP via WhatsApp to {}: {}", phoneNumber, e.getMessage());
            log.error("W-OTP check: url={}, error={}", url, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
