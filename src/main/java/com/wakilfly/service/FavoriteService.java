package com.wakilfly.service;

import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.ProductResponse;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.Favorite;
import com.wakilfly.model.Product;
import com.wakilfly.model.User;
import com.wakilfly.repository.FavoriteRepository;
import com.wakilfly.repository.ProductRepository;
import com.wakilfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Add product to favorites
     */
    @Transactional
    public void addToFavorites(UUID userId, UUID productId) {
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new RuntimeException("Product already in favorites");
        }

        User user = userRepository.getReferenceById(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        Favorite favorite = Favorite.builder()
                .user(user)
                .product(product)
                .build();

        favoriteRepository.save(favorite);
    }

    /**
     * Remove product from favorites
     */
    @Transactional
    public void removeFromFavorites(UUID userId, UUID productId) {
        favoriteRepository.deleteByUserIdAndProductId(userId, productId);
    }

    /**
     * Check if product is in favorites
     */
    public boolean isFavorite(UUID userId, UUID productId) {
        return favoriteRepository.existsByUserIdAndProductId(userId, productId);
    }

    /**
     * Get user's favorite products
     */
    public PagedResponse<ProductResponse> getFavorites(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return PagedResponse.<ProductResponse>builder()
                .content(favorites.getContent().stream()
                        .map(f -> mapToProductResponse(f.getProduct()))
                        .collect(Collectors.toList()))
                .page(favorites.getNumber())
                .size(favorites.getSize())
                .totalElements(favorites.getTotalElements())
                .totalPages(favorites.getTotalPages())
                .last(favorites.isLast())
                .first(favorites.isFirst())
                .build();
    }

    /**
     * Get favorite count for a product
     */
    public long getFavoriteCount(UUID productId) {
        return favoriteRepository.countByProductId(productId);
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .compareAtPrice(product.getCompareAtPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .isActive(product.getIsActive())
                .viewsCount(product.getViewsCount())
                .ordersCount(product.getOrdersCount())
                .rating(product.getRating())
                .reviewsCount(product.getReviewsCount())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
