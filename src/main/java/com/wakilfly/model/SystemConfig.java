package com.wakilfly.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "system_config", uniqueConstraints = @UniqueConstraint(columnNames = "config_key"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "config_key", nullable = false, unique = true, length = 64)
    private String configKey;

    @Column(name = "config_value", nullable = false, length = 255)
    private String configValue;

    public static final String KEY_AGENT_REGISTER_AMOUNT = "agent_register_amount";
    public static final String KEY_BUSINESS_ACTIVATION_AMOUNT = "business_activation_amount";
    public static final String DEFAULT_AGENT_REGISTER_AMOUNT = "20000.00";
    public static final String DEFAULT_BUSINESS_ACTIVATION_AMOUNT = "10000.00";
}
