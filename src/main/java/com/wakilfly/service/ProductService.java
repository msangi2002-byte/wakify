package com.wakilfly.service;

import com.wakilfly.dto.request.CreateProductRequest;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.ProductResponse;
import com.wakilfly.model.Business;
import com.wakilfly.model.Product;
import com.wakilfly.model.ProductImage;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.repository.BusinessRepository;
import com.wakilfly.repository.ProductImageRepository;
import com.wakilfly.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final BusinessRepository businessRepository;
    private final FileStorageService fileStorageService;

    /**
     * Create a new product for a business
     */
    @Transactional
    public ProductResponse createProduct(UUID businessId, CreateProductRequest request, List<MultipartFile> images) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));

        Product product = Product.builder()
                .business(business)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .compareAtPrice(request.getCompareAtPrice())
                .category(request.getCategory())
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                .trackStock(request.getTrackStock() != null && request.getTrackStock())
                .isActive(true)
                .build();

        product = productRepository.save(product);

        // Save product images
        if (images != null && !images.isEmpty()) {
            int order = 0;
            for (MultipartFile file : images) {
                String url = fileStorageService.storeFile(file, "products");
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .url(url)
                        .isPrimary(order == 0)
                        .displayOrder(order++)
                        .build();
                productImageRepository.save(image);
            }
        }

        log.info("Product {} created for business {}", product.getId(), businessId);
        return mapToProductResponse(product);
    }

    /**
     * Get product by ID
     */
    public ProductResponse getProductById(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        // Increment view count
        product.setViewsCount(product.getViewsCount() + 1);
        productRepository.save(product);

        return mapToProductResponse(product);
    }

    /**
     * Update a product
     */
    @Transactional
    public ProductResponse updateProduct(UUID productId, UUID businessId, CreateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (!product.getBusiness().getId().equals(businessId)) {
            throw new BadRequestException("You can only update your own products");
        }

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getCompareAtPrice() != null) {
            product.setCompareAtPrice(request.getCompareAtPrice());
        }
        if (request.getCategory() != null) {
            product.setCategory(request.getCategory());
        }
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }
        if (request.getTrackStock() != null) {
            product.setTrackStock(request.getTrackStock());
        }

        product = productRepository.save(product);
        return mapToProductResponse(product);
    }

    /**
     * Delete a product (soft delete)
     */
    @Transactional
    public void deleteProduct(UUID productId, UUID businessId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (!product.getBusiness().getId().equals(businessId)) {
            throw new BadRequestException("You can only delete your own products");
        }

        product.setIsActive(false);
        productRepository.save(product);
        log.info("Product {} deleted", productId);
    }

    /**
     * Get products by business
     */
    public PagedResponse<ProductResponse> getBusinessProducts(UUID businessId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.findByBusinessIdAndIsActiveTrue(businessId, pageable);

        return buildPagedResponse(products);
    }

    /**
     * Get all products (marketplace)
     */
    public PagedResponse<ProductResponse> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.findByIsActiveTrue(pageable);

        return buildPagedResponse(products);
    }

    /**
     * Search products
     */
    public PagedResponse<ProductResponse> searchProducts(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.searchProducts(query, pageable);

        return buildPagedResponse(products);
    }

    /**
     * Get products by category
     */
    public PagedResponse<ProductResponse> getProductsByCategory(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.findByCategory(category, pageable);

        return buildPagedResponse(products);
    }

    /**
     * Get trending products
     */
    public PagedResponse<ProductResponse> getTrendingProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.findTrending(pageable);

        return buildPagedResponse(products);
    }

    /**
     * Get products by region
     */
    public PagedResponse<ProductResponse> getProductsByRegion(String region, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.findByRegion(region, pageable);

        return buildPagedResponse(products);
    }

    // Helper methods

    private PagedResponse<ProductResponse> buildPagedResponse(Page<Product> products) {
        return PagedResponse.<ProductResponse>builder()
                .content(products.getContent().stream()
                        .map(this::mapToProductResponse)
                        .collect(Collectors.toList()))
                .page(products.getNumber())
                .size(products.getSize())
                .totalElements(products.getTotalElements())
                .totalPages(products.getTotalPages())
                .last(products.isLast())
                .first(products.isFirst())
                .build();
    }

    private ProductResponse mapToProductResponse(Product product) {
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        Business business = product.getBusiness();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .compareAtPrice(product.getCompareAtPrice())
                .category(product.getCategory())
                .stockQuantity(product.getStockQuantity())
                .inStock(product.isInStock())
                .isActive(product.getIsActive())
                .images(images.stream()
                        .map(img -> ProductResponse.ImageResponse.builder()
                                .id(img.getId())
                                .url(img.getUrl())
                                .isPrimary(img.getIsPrimary())
                                .displayOrder(img.getDisplayOrder())
                                .build())
                        .collect(Collectors.toList()))
                .business(ProductResponse.BusinessSummary.builder()
                        .id(business.getId())
                        .name(business.getName())
                        .logo(business.getLogo())
                        .region(business.getRegion())
                        .isVerified(business.getIsVerified())
                        .build())
                .viewsCount(product.getViewsCount())
                .rating(product.getRating())
                .reviewsCount(product.getReviewsCount())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
