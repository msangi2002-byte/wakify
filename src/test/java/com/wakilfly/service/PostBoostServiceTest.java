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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostBoostServiceTest {

    @Mock private SystemSettingsService systemSettingsService;
    @Mock private PaymentService paymentService;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private PromotionRepository promotionRepository;
    @Mock private BusinessRepository businessRepository;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks
    private PostBoostService postBoostService;

    private User user;
    private Post post;
    private UUID userId;
    private UUID postId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        postId = UUID.randomUUID();
        user = User.builder().id(userId).name("Test User").phone("+255712345678").build();
        post = Post.builder().id(postId).author(user).caption("Test post").build();
    }

    @Test
    void getAdsPricePerPerson_ShouldDelegateToSettings() {
        when(systemSettingsService.getAdsPricePerPerson()).thenReturn(new BigDecimal("2.00"));
        BigDecimal result = postBoostService.getAdsPricePerPerson();
        assertEquals(new BigDecimal("2.00"), result);
    }

    @Test
    void createPostBoost_ShouldThrow_WhenPostNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                postBoostService.createPostBoost(userId, postId, 1000, "+255712345678",
                        PromotionObjective.ENGAGEMENT, "AUTOMATIC", null, null, null, null, null));
    }

    @Test
    void createPostBoost_ShouldThrow_WhenTargetReachTooLow() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThrows(BadRequestException.class, () ->
                postBoostService.createPostBoost(userId, postId, 0, "+255712345678",
                        PromotionObjective.ENGAGEMENT, "AUTOMATIC", null, null, null, null, null));
    }
}
