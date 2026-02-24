package com.wakilfly.service;

import com.wakilfly.dto.request.AdminSettingsUpdateRequest;
import com.wakilfly.dto.response.AdminSettingsResponse;
import com.wakilfly.model.SystemSetting;
import com.wakilfly.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingsService {

    public static final String KEY_AGENT_REGISTER_AMOUNT = "agent_register_amount";
    public static final String KEY_TO_BE_BUSINESS_AMOUNT = "to_be_business_amount";
    public static final String KEY_ADS_PRICE_PER_PERSON = "ads_price_per_person";
    public static final String KEY_PAYMENT_DEMO_MODE = "payment_demo_mode";
    /** When true, all sponsored promotions are free (no payment required). */
    public static final String KEY_SPONSORED_FREE_MODE = "sponsored_free_mode";
    private static final BigDecimal DEFAULT_AGENT_REGISTER = new BigDecimal("20000.00");
    private static final BigDecimal DEFAULT_TO_BE_BUSINESS = new BigDecimal("10000.00");
    private static final BigDecimal DEFAULT_ADS_PRICE_PER_PERSON = new BigDecimal("2.00");

    private final SystemSettingsRepository systemSettingsRepository;

    public AdminSettingsResponse getSettings() {
        BigDecimal agentAmount = getAmount(KEY_AGENT_REGISTER_AMOUNT, DEFAULT_AGENT_REGISTER);
        BigDecimal businessAmount = getAmount(KEY_TO_BE_BUSINESS_AMOUNT, DEFAULT_TO_BE_BUSINESS);
        BigDecimal adsPrice = getAmount(KEY_ADS_PRICE_PER_PERSON, DEFAULT_ADS_PRICE_PER_PERSON);
        return AdminSettingsResponse.builder()
                .agentRegisterAmount(agentAmount)
                .toBeBusinessAmount(businessAmount)
                .adsPricePerPerson(adsPrice)
                .paymentDemoMode(getPaymentDemoMode())
                .build();
    }

    @Transactional
    public AdminSettingsResponse updateSettings(AdminSettingsUpdateRequest request) {
        setAmount(KEY_AGENT_REGISTER_AMOUNT, request.getAgentRegisterAmount());
        setAmount(KEY_TO_BE_BUSINESS_AMOUNT, request.getToBeBusinessAmount());
        setAmount(KEY_ADS_PRICE_PER_PERSON, request.getAdsPricePerPerson());
        if (request.getPaymentDemoMode() != null) {
            setBoolean(KEY_PAYMENT_DEMO_MODE, request.getPaymentDemoMode());
        }
        return getSettings();
    }

    /** Used by AgentService for registration fee. */
    public BigDecimal getAgentRegisterAmount() {
        return getAmount(KEY_AGENT_REGISTER_AMOUNT, DEFAULT_AGENT_REGISTER);
    }

    /** Used by AgentService for business activation fee. */
    public BigDecimal getToBeBusinessAmount() {
        return getAmount(KEY_TO_BE_BUSINESS_AMOUNT, DEFAULT_TO_BE_BUSINESS);
    }

    /** Used by PostBoostService for ads price per person. */
    public BigDecimal getAdsPricePerPerson() {
        return getAmount(KEY_ADS_PRICE_PER_PERSON, DEFAULT_ADS_PRICE_PER_PERSON);
    }

    /** When true, PaymentService skips HarakaPay and marks payment SUCCESS immediately (demo mode). */
    public boolean getPaymentDemoMode() {
        return getBoolean(KEY_PAYMENT_DEMO_MODE, false);
    }

    /** When true, all new sponsored promotions are free (no payment; promotions go ACTIVE immediately). */
    public boolean getSponsoredFreeMode() {
        return getBoolean(KEY_SPONSORED_FREE_MODE, false);
    }

    /** Set sponsored free mode (admin only). */
    public void setSponsoredFreeMode(boolean value) {
        setBoolean(KEY_SPONSORED_FREE_MODE, value);
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        return systemSettingsRepository.findByKey(key)
                .map(s -> "true".equalsIgnoreCase(s.getValue().trim()))
                .orElse(defaultValue);
    }

    private void setBoolean(String key, boolean value) {
        String valueStr = value ? "true" : "false";
        SystemSetting setting = systemSettingsRepository.findByKey(key)
                .orElseGet(() -> {
                    SystemSetting s = new SystemSetting();
                    s.setId(UUID.randomUUID());
                    s.setKey(key);
                    return s;
                });
        setting.setValue(valueStr);
        setting.setUpdatedAt(LocalDateTime.now());
        systemSettingsRepository.save(setting);
    }

    private BigDecimal getAmount(String key, BigDecimal defaultValue) {
        return systemSettingsRepository.findByKey(key)
                .map(s -> {
                    try {
                        return new BigDecimal(s.getValue().trim());
                    } catch (NumberFormatException e) {
                        log.warn("Invalid system_settings value for key {}: {}", key, s.getValue());
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    private void setAmount(String key, BigDecimal value) {
        String valueStr = value.stripTrailingZeros().toPlainString();
        SystemSetting setting = systemSettingsRepository.findByKey(key)
                .orElseGet(() -> {
                    SystemSetting s = new SystemSetting();
                    s.setId(UUID.randomUUID());
                    s.setKey(key);
                    return s;
                });
        setting.setValue(valueStr);
        setting.setUpdatedAt(LocalDateTime.now());
        systemSettingsRepository.save(setting);
    }
}
