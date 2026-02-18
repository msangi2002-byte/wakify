package com.wakilfly.service;

import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.*;
import com.wakilfly.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock private PromotionRepository promotionRepository;
    @Mock private PromotionPackageRepository packageRepository;
    @Mock private UserRepository userRepository;
    @Mock private BusinessRepository businessRepository;
    @Mock private PostRepository postRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentService paymentService;

    @InjectMocks
    private PromotionService promotionService;

    private User user;
    private Business business;
    private Product product;
    private UUID userId;
    private UUID productId;
    private UUID businessId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        businessId = UUID.randomUUID();

        user = User.builder().id(userId).name("Test User").phone("+255712345678").build();
        business = Business.builder().id(businessId).name("Test Shop").owner(user).build();
        product = Product.builder().id(productId).name("Test Product").business(business).build();
    }

    @Test
    void getPackages_ShouldReturnList_WhenCalled() {
        when(packageRepository.findByIsActiveTrueOrderBySortOrderAsc()).thenReturn(java.util.List.of());
        var result = promotionService.getPackages(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void pausePromotion_ShouldThrow_WhenNotActive() {
        Promotion promo = Promotion.builder()
                .id(UUID.randomUUID())
                .user(user)
                .status(PromotionStatus.PENDING)
                .build();
        when(promotionRepository.findById(any())).thenReturn(Optional.of(promo));

        assertThrows(BadRequestException.class, () ->
                promotionService.pausePromotion(promo.getId(), userId));
    }

    @Test
    void cancelPromotion_ShouldThrow_WhenCompleted() {
        Promotion promo = Promotion.builder()
                .id(UUID.randomUUID())
                .user(user)
                .status(PromotionStatus.COMPLETED)
                .build();
        when(promotionRepository.findById(any())).thenReturn(Optional.of(promo));

        assertThrows(BadRequestException.class, () ->
                promotionService.cancelPromotion(promo.getId(), userId));
    }
}
