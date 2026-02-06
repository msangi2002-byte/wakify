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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public CartResponse getCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        return mapToResponse(cart);
    }

    @Transactional
    public CartResponse addToCart(UUID userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(request.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.addItem(newItem);
        }

        cart = cartRepository.save(cart);
        return mapToResponse(cart);
    }

    @Transactional
    public CartResponse removeFromCart(UUID userId, UUID productId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        cartRepository.save(cart);
        return mapToResponse(cart);
    }

    @Transactional
    public void clearCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.getReferenceById(userId);
                    Cart newCart = Cart.builder().user(user).build();
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse mapToResponse(Cart cart) {
        BigDecimal summaryTotal = BigDecimal.ZERO;
        int itemCount = 0;

        List<CartResponse.CartItemResponse> items = cart.getItems().stream().map(item -> {
            BigDecimal itemTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return CartResponse.CartItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .productImage(item.getProduct().getThumbnail())
                    .price(item.getProduct().getPrice())
                    .quantity(item.getQuantity())
                    .total(itemTotal)
                    .build();
        }).collect(Collectors.toList());

        for (CartResponse.CartItemResponse i : items) {
            summaryTotal = summaryTotal.add(i.getTotal());
            itemCount += i.getQuantity();
        }

        return CartResponse.builder()
                .id(cart.getId())
                .items(items)
                .summaryTotal(summaryTotal)
                .itemCount(itemCount)
                .build();
    }
}
