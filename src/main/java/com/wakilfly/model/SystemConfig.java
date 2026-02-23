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

    /** Creator receives this fraction of gift value as diamonds (platform keeps the rest). */
    public static final String KEY_CREATOR_SPLIT_RATIO = "creator_split_ratio";
    public static final String DEFAULT_CREATOR_SPLIT_RATIO = "0.50";
    /** 1 diamond = X TZS when converting to withdrawable cash (creator cashout). Align with coin value: ~43 TZS. */
    public static final String KEY_DIAMOND_TO_TZS_RATE = "diamond_to_tzs_rate";
    public static final String DEFAULT_DIAMOND_TO_TZS_RATE = "43";
    /** Display: 1 coin ≈ X TZS when buying coins (in-app / Tanzania market). */
    public static final String KEY_COIN_TO_TZS_BUY_RATE = "coin_to_tzs_buy_rate";
    public static final String DEFAULT_COIN_TO_TZS_BUY_RATE = "43";
    public static final String KEY_WITHDRAWAL_MIN_TZS = "withdrawal_min_tzs";
    public static final String DEFAULT_WITHDRAWAL_MIN_TZS = "1000";
    public static final String KEY_WITHDRAWAL_MAX_TZS = "withdrawal_max_tzs";
    public static final String DEFAULT_WITHDRAWAL_MAX_TZS = "5000000";
}
