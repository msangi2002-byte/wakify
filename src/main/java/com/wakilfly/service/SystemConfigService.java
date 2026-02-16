package com.wakilfly.service;

import com.wakilfly.exception.BadRequestException;
import com.wakilfly.model.SystemConfig;
import com.wakilfly.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    public BigDecimal getAgentRegisterAmount() {
        return getAmount(SystemConfig.KEY_AGENT_REGISTER_AMOUNT, SystemConfig.DEFAULT_AGENT_REGISTER_AMOUNT);
    }

    public BigDecimal getBusinessActivationAmount() {
        return getAmount(SystemConfig.KEY_BUSINESS_ACTIVATION_AMOUNT, SystemConfig.DEFAULT_BUSINESS_ACTIVATION_AMOUNT);
    }

    private BigDecimal getAmount(String key, String defaultValue) {
        return systemConfigRepository.findByConfigKey(key)
                .map(c -> new BigDecimal(c.getConfigValue().trim()))
                .orElse(new BigDecimal(defaultValue));
    }

    @Transactional
    public Map<String, BigDecimal> updateFeeAmounts(BigDecimal agentRegisterAmount, BigDecimal businessActivationAmount) {
        if (agentRegisterAmount != null && agentRegisterAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Agent register amount must be non-negative");
        }
        if (businessActivationAmount != null && businessActivationAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Business activation amount must be non-negative");
        }
        Map<String, BigDecimal> result = new HashMap<>();
        if (agentRegisterAmount != null) {
            saveOrUpdate(SystemConfig.KEY_AGENT_REGISTER_AMOUNT, agentRegisterAmount.toPlainString());
            result.put("agentRegisterAmount", agentRegisterAmount);
        }
        if (businessActivationAmount != null) {
            saveOrUpdate(SystemConfig.KEY_BUSINESS_ACTIVATION_AMOUNT, businessActivationAmount.toPlainString());
            result.put("businessActivationAmount", businessActivationAmount);
        }
        return result;
    }

    public Map<String, BigDecimal> getFeeAmounts() {
        Map<String, BigDecimal> map = new HashMap<>();
        map.put("agentRegisterAmount", getAgentRegisterAmount());
        map.put("businessActivationAmount", getBusinessActivationAmount());
        return map;
    }

    private void saveOrUpdate(String key, String value) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElse(SystemConfig.builder().configKey(key).configValue(value).build());
        config.setConfigValue(value);
        systemConfigRepository.save(config);
    }
}
