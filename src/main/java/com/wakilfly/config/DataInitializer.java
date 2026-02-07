package com.wakilfly.config;

import com.wakilfly.model.CoinPackage;
import com.wakilfly.model.Role;
import com.wakilfly.model.User;
import com.wakilfly.model.VirtualGift;
import com.wakilfly.repository.CoinPackageRepository;
import com.wakilfly.repository.UserRepository;
import com.wakilfly.repository.VirtualGiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final VirtualGiftRepository giftRepository;
    private final CoinPackageRepository packageRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (giftRepository.count() == 0) {
            seedGifts();
        }
        if (packageRepository.count() == 0) {
            seedPackages();
        }
        seedTestUserIfMissing();
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

    private void seedGifts() {
        VirtualGift rose = VirtualGift.builder()
                .name("Rose")
                .description("A beautiful red rose")
                .price(new BigDecimal("200.00"))
                .coinValue(5)
                .sortOrder(1)
                .iconUrl("https://api.wakify.com/assets/gifts/rose.png")
                .build();

        VirtualGift heart = VirtualGift.builder()
                .name("Heart")
                .description("Love is in the air")
                .price(new BigDecimal("500.00"))
                .coinValue(15)
                .sortOrder(2)
                .iconUrl("https://api.wakify.com/assets/gifts/heart.png")
                .build();

        VirtualGift crown = VirtualGift.builder()
                .name("Crown")
                .description("For the king/queen")
                .price(new BigDecimal("5000.00"))
                .coinValue(150)
                .sortOrder(3)
                .isPremium(true)
                .iconUrl("https://api.wakify.com/assets/gifts/crown.png")
                .build();

        VirtualGift diamond = VirtualGift.builder()
                .name("Diamond")
                .description("Shine bright like a diamond")
                .price(new BigDecimal("10000.00"))
                .coinValue(300)
                .sortOrder(4)
                .isPremium(true)
                .iconUrl("https://api.wakify.com/assets/gifts/diamond.png")
                .build();

        giftRepository.saveAll(Arrays.asList(rose, heart, crown, diamond));
    }

    private void seedPackages() {
        CoinPackage p1 = CoinPackage.builder()
                .name("Starter Pack")
                .coinAmount(50)
                .price(new BigDecimal("2000.00"))
                .description("Get started with 50 coins")
                .sortOrder(1)
                .build();

        CoinPackage p2 = CoinPackage.builder()
                .name("Value Pack")
                .coinAmount(150)
                .bonusCoins(20)
                .price(new BigDecimal("5000.00"))
                .description("Better value for money")
                .isPopular(true)
                .sortOrder(2)
                .build();

        CoinPackage p3 = CoinPackage.builder()
                .name("Whale Pack")
                .coinAmount(500)
                .bonusCoins(100)
                .price(new BigDecimal("15000.00"))
                .description("For the big spenders")
                .sortOrder(3)
                .build();

        packageRepository.saveAll(Arrays.asList(p1, p2, p3));
    }
}
