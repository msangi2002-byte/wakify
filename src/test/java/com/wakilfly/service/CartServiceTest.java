package com.wakilfly.service;

import com.wakilfly.dto.request.AddToCartRequest;
import com.wakilfly.dto.response.CartResponse;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.Cart;
import com.wakilfly.model.CartItem;
import com.wakilfly.model.Product;
import com.wakilfly.model.User;
import com.wakilfly.repository.CartRepository;
import com.wakilfly.repository.ProductRepository;
import com.wakilfly.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User user;
    private Product product;
    private Cart cart;
    private UUID userId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();

        user = User.builder().id(userId).name("Test User").build();

        product = Product.builder()
                .id(productId)
                .name("Test Product")
                .price(new BigDecimal("1000.00"))
                .stockQuantity(10)
                .build();

        cart = Cart.builder()
                .id(UUID.randomUUID())
                .user(user)
                .items(new ArrayList<>())
                .build();
    }

    @Test
    void getCart_ShouldReturnCartResponse_WhenCartExists() {
        // Arrange
        CartItem item = CartItem.builder()
                .id(UUID.randomUUID())
                .cart(cart)
                .product(product)
                .quantity(2)
                .build();
        cart.addItem(item);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Act
        CartResponse response = cartService.getCart(userId);

        // Assert
        assertNotNull(response);
        assertEquals(cart.getId(), response.getId());
        assertEquals(1, response.getItems().size());
        assertEquals(2, response.getItemCount());

        // Total = 1000 * 2 = 2000
        assertEquals(new BigDecimal("2000.00"), response.getSummaryTotal());
    }

    @Test
    void addToCart_ShouldAddNewItem_WhenItemDoesNotExist() {
        // Arrange
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(productId);
        request.setQuantity(1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CartResponse response = cartService.addToCart(userId, request);

        // Assert
        assertEquals(1, response.getItems().size());
        assertEquals("Test Product", response.getItems().get(0).getProductName());
        assertEquals(1, response.getItems().get(0).getQuantity());
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void addToCart_ShouldUpdateQuantity_WhenItemExists() {
        // Arrange
        // Pre-populate cart with 1 item
        CartItem item = CartItem.builder()
                .id(UUID.randomUUID())
                .cart(cart)
                .product(product)
                .quantity(1)
                .build();
        cart.addItem(item);

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(productId);
        request.setQuantity(3);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CartResponse response = cartService.addToCart(userId, request);

        // Assert
        assertEquals(1, response.getItems().size()); // Still 1 unique item
        assertEquals(4, response.getItems().get(0).getQuantity()); // 1 + 3 = 4

        // Total = 1000 * 4 = 4000
        assertEquals(new BigDecimal("4000.00"), response.getSummaryTotal());
    }

    @Test
    void removeFromCart_ShouldRemoveItem() {
        // Arrange
        CartItem item = CartItem.builder()
                .id(UUID.randomUUID())
                .cart(cart)
                .product(product)
                .quantity(1)
                .build();
        cart.addItem(item);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CartResponse response = cartService.removeFromCart(userId, productId);

        // Assert
        assertTrue(response.getItems().isEmpty());
        assertEquals(0, response.getItemCount());
        assertEquals(BigDecimal.ZERO, response.getSummaryTotal());
    }
}
