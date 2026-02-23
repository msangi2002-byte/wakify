package com.wakilfly.config;

import com.wakilfly.model.CoinPackage;
import com.wakilfly.model.Role;
import com.wakilfly.model.SystemConfig;
import com.wakilfly.model.User;
import com.wakilfly.model.VirtualGift;
import com.wakilfly.repository.CoinPackageRepository;
import com.wakilfly.repository.SystemConfigRepository;
import com.wakilfly.repository.UserRepository;
import com.wakilfly.repository.VirtualGiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final VirtualGiftRepository giftRepository;
    private final CoinPackageRepository packageRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemConfigRepository systemConfigRepository;
    private final com.wakilfly.service.AdminRoleDefinitionService adminRoleDefinitionService;

    @Override
    public void run(String... args) {
        seedWakilfyGifts(); // 10 TikTok-style gifts (upsert by name)
        if (packageRepository.count() == 0) {
            seedPackages();
        }
        seedSystemConfigIfMissing();
        adminRoleDefinitionService.seedBuiltinIfEmpty();
        seedTestUserIfMissing();
    }

    private void seedSystemConfigIfMissing() {
        if (systemConfigRepository.findByConfigKey(SystemConfig.KEY_AGENT_REGISTER_AMOUNT).isEmpty()) {
            systemConfigRepository.save(SystemConfig.builder()
                    .configKey(SystemConfig.KEY_AGENT_REGISTER_AMOUNT)
                    .configValue(SystemConfig.DEFAULT_AGENT_REGISTER_AMOUNT)
                    .build());
        }
        if (systemConfigRepository.findByConfigKey(SystemConfig.KEY_BUSINESS_ACTIVATION_AMOUNT).isEmpty()) {
            systemConfigRepository.save(SystemConfig.builder()
                    .configKey(SystemConfig.KEY_BUSINESS_ACTIVATION_AMOUNT)
                    .configValue(SystemConfig.DEFAULT_BUSINESS_ACTIVATION_AMOUNT)
                    .build());
        }
        if (systemConfigRepository.findByConfigKey(SystemConfig.KEY_CREATOR_SPLIT_RATIO).isEmpty()) {
            systemConfigRepository.save(SystemConfig.builder()
                    .configKey(SystemConfig.KEY_CREATOR_SPLIT_RATIO)
                    .configValue(SystemConfig.DEFAULT_CREATOR_SPLIT_RATIO)
                    .build());
        }
        if (systemConfigRepository.findByConfigKey(SystemConfig.KEY_DIAMOND_TO_TZS_RATE).isEmpty()) {
            systemConfigRepository.save(SystemConfig.builder()
                    .configKey(SystemConfig.KEY_DIAMOND_TO_TZS_RATE)
                    .configValue(SystemConfig.DEFAULT_DIAMOND_TO_TZS_RATE)
                    .build());
        }
        if (systemConfigRepository.findByConfigKey(SystemConfig.KEY_WITHDRAWAL_MIN_TZS).isEmpty()) {
            systemConfigRepository.save(SystemConfig.builder()
                    .configKey(SystemConfig.KEY_WITHDRAWAL_MIN_TZS)
                    .configValue(SystemConfig.DEFAULT_WITHDRAWAL_MIN_TZS)
                    .build());
        }
        if (systemConfigRepository.findByConfigKey(SystemConfig.KEY_WITHDRAWAL_MAX_TZS).isEmpty()) {
            systemConfigRepository.save(SystemConfig.builder()
                    .configKey(SystemConfig.KEY_WITHDRAWAL_MAX_TZS)
                    .configValue(SystemConfig.DEFAULT_WITHDRAWAL_MAX_TZS)
                    .build());
        }
        if (systemConfigRepository.findByConfigKey(SystemConfig.KEY_COIN_TO_TZS_BUY_RATE).isEmpty()) {
            systemConfigRepository.save(SystemConfig.builder()
                    .configKey(SystemConfig.KEY_COIN_TO_TZS_BUY_RATE)
                    .configValue(SystemConfig.DEFAULT_COIN_TO_TZS_BUY_RATE)
                    .build());
        }
    }

    /** Test user for login API testing: phone 255712000000, password test123 (or email test@wakilfy.com) */
    private void seedTestUserIfMissing() {
        if (userRepository.findByPhone("255712000000").isPresent()) {
            return;
        }
        User test = User.builder()
                .name("Test User")
                .email("test@wakilfy.com")
                .phone("255712000000")
                .password(passwordEncoder.encode("test123"))
                .role(Role.USER)
                .isVerified(true)
                .isActive(true)
                .build();
        userRepository.save(test);
    }

    /** 1 coin ≈ 43 TZS (Tanzania in-app). Price in TZS = coinValue * 43 for display. */
    private static final int COIN_TO_TZS = 43;

    /** Seed/upsert 10 Wakilfy gifts (TikTok-style). Logic: 1 coin ≈ 43 TZS; creator gets 50% as diamonds. */
    private void seedWakilfyGifts() {
        List<VirtualGift> gifts = Arrays.asList(
                gift("Rose", 1, "Basic", "Gift ya kawaida kabisa, hutumika kuonyesha uwepo.", 1, false),
                gift("Finger Heart", 5, "Low", "Ishara ya upendo mdogo.", 2, false),
                gift("Doughnut", 30, "Low", "Gift ya katikati kwa watazamaji wa kawaida.", 3, false),
                gift("Paper Crane", 99, "Mid", "Gift nzuri inayoonekana vizuri kwenye screen.", 4, false),
                gift("Money Gun", 500, "Popular", "Maarufu sana, ina animation ya kurusha pesa.", 5, true),
                gift("Galaxy", 1000, "High", "Inaonyesha animation kubwa ya anga.", 6, true),
                gift("Whale Diving", 2150, "High", "Animation ya Nyangumi akiruka majini.", 7, true),
                gift("Leon the Kitten", 4888, "Premium", "Paka mkubwa na animation nzuri.", 8, true),
                gift("Lion", 29999, "Ultra", "Maarufu kama Simba. Heshima kubwa kwa Creator.", 9, true),
                gift("Wakilfy Universe", 34999, "Elite", "Baba Lao. Ghali zaidi na animation ya muda mrefu.", 10, true)
        );
        for (VirtualGift g : gifts) {
            giftRepository.findByName(g.getName())
                    .ifPresentOrElse(
                            existing -> {
                                existing.setCoinValue(g.getCoinValue());
                                existing.setPrice(g.getPrice());
                                existing.setDescription(g.getDescription());
                                existing.setLevel(g.getLevel());
                                existing.setSortOrder(g.getSortOrder());
                                existing.setIsPremium(g.getIsPremium());
                                if (g.getIconUrl() != null) existing.setIconUrl(g.getIconUrl());
                                giftRepository.save(existing);
                            },
                            () -> giftRepository.save(g)
                    );
        }
    }

    private VirtualGift gift(String name, int coinValue, String level, String description, int sortOrder, boolean isPremium) {
        return VirtualGift.builder()
                .name(name)
                .coinValue(coinValue)
                .price(new BigDecimal(coinValue * COIN_TO_TZS))
                .level(level)
                .description(description)
                .sortOrder(sortOrder)
                .isPremium(isPremium)
                .isActive(true)
                .iconUrl(null) // set CDN path when assets ready
                .build();
    }

    /** Coin packages: 1 coin ≈ 43 TZS (Tanzania in-app). 70 coins ≈ $1; 65 coins ≈ 2,800 TZS. */
    private void seedPackages() {
        int rate = 43;
        CoinPackage p1 = CoinPackage.builder()
                .name("Starter Pack")
                .coinAmount(65)
                .price(new BigDecimal("2795"))   // 65 × 43
                .description("65 coins – kuanza kutumia gifts")
                .sortOrder(1)
                .build();

        CoinPackage p2 = CoinPackage.builder()
                .name("Value Pack")
                .coinAmount(350)
                .bonusCoins(0)
                .price(new BigDecimal("15050"))  // 350 × 43
                .description("350 coins – thamani nzuri")
                .isPopular(true)
                .sortOrder(2)
                .build();

        CoinPackage p3 = CoinPackage.builder()
                .name("Popular Pack")
                .coinAmount(1000)
                .bonusCoins(50)
                .price(new BigDecimal("43000")) // 1000 × 43
                .description("1,000 + 50 bonus coins")
                .isPopular(true)
                .sortOrder(3)
                .build();

        CoinPackage p4 = CoinPackage.builder()
                .name("Whale Pack")
                .coinAmount(5000)
                .bonusCoins(500)
                .price(new BigDecimal("215000"))
                .description("5,000 + 500 bonus – kwa wakubwa")
                .sortOrder(4)
                .build();

        packageRepository.saveAll(Arrays.asList(p1, p2, p3, p4));
    }
}
