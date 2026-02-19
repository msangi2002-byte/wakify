package com.wakilfly.service;

import com.wakilfly.dto.request.UpdateBusinessRequest;
import com.wakilfly.dto.response.*;
import com.wakilfly.model.*;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessFollowRepository businessFollowRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final OrderRepository orderRepository;

    /**
     * Get business by ID (optionally with isFollowing for current user)
     */
    public BusinessResponse getBusinessById(UUID businessId, UUID currentUserId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));
        return mapToBusinessResponse(business, currentUserId);
    }

    /**
     * Get business by ID (no current user - for public)
     */
    public BusinessResponse getBusinessById(UUID businessId) {
        return getBusinessById(businessId, null);
    }

    /**
     * Get business by owner ID
     */
    public BusinessResponse getBusinessByOwnerId(UUID ownerId) {
        Business business = businessRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found for this user"));
        return mapToBusinessResponse(business, ownerId);
    }

    @Transactional
    public void followBusiness(UUID userId, UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));
        if (businessFollowRepository.existsByUserIdAndBusinessId(userId, businessId)) return;
        User user = userRepository.getReferenceById(userId);
        businessFollowRepository.save(BusinessFollow.builder().user(user).business(business).build());
        business.setFollowersCount(business.getFollowersCount() != null ? business.getFollowersCount() + 1 : 1);
        businessRepository.save(business);
        log.info("User {} followed business {}", userId, businessId);
    }

    @Transactional
    public void unfollowBusiness(UUID userId, UUID businessId) {
        if (!businessFollowRepository.existsByUserIdAndBusinessId(userId, businessId)) return;
        businessFollowRepository.deleteByUserIdAndBusinessId(userId, businessId);
        Business business = businessRepository.findById(businessId).orElse(null);
        if (business != null) {
            business.setFollowersCount(Math.max(0, (business.getFollowersCount() != null ? business.getFollowersCount() : 1) - 1));
            businessRepository.save(business);
        }
        log.info("User {} unfollowed business {}", userId, businessId);
    }

    /**
     * Get Business Dashboard Summary
     */
    @Transactional(readOnly = true)
    public BusinessDashboardResponse getBusinessDashboard(UUID ownerId) {
        Business business = businessRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        UUID businessId = business.getId();
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();

        BigDecimal totalRevenue = orderRepository.sumTotalSalesByBusinessId(businessId);
        BigDecimal todayRevenue = orderRepository.sumSalesByBusinessIdSince(businessId, startOfToday);
        BigDecimal monthRevenue = orderRepository.sumSalesByBusinessIdSince(businessId, startOfMonth);

        long totalOrders = orderRepository.countByBusinessId(businessId);
        long pendingOrders = orderRepository.countByBusinessIdAndStatus(businessId, OrderStatus.PENDING);
        long completedOrders = orderRepository.countByBusinessIdAndStatus(businessId, OrderStatus.DELIVERED);

        long totalProducts = productRepository.countByBusinessId(businessId);
        long activeProducts = productRepository.countActiveByBusinessId(businessId);

        return BusinessDashboardResponse.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .monthRevenue(monthRevenue != null ? monthRevenue : BigDecimal.ZERO)
                .totalOrders((int) totalOrders)
                .pendingOrders((int) pendingOrders)
                .completedOrders((int) completedOrders)
                .totalProducts((int) totalProducts)
                .activeProducts((int) activeProducts)
                .totalViews(0) // Business views not tracked yet
                .averageRating(business.getRating())
                .totalReviews(business.getReviewsCount())
                .build();
    }

    /**
     * Update Business Profile
     */
    @Transactional
    public BusinessResponse updateBusiness(UUID ownerId, UpdateBusinessRequest request) {
        Business business = businessRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        if (request.getName() != null)
            business.setName(request.getName());
        if (request.getDescription() != null)
            business.setDescription(request.getDescription());
        if (request.getCategory() != null)
            business.setCategory(request.getCategory());
        if (request.getPhone() != null)
            business.setPhone(request.getPhone());
        if (request.getEmail() != null)
            business.setEmail(request.getEmail());
        if (request.getWebsite() != null)
            business.setWebsite(request.getWebsite());
        if (request.getRegion() != null)
            business.setRegion(request.getRegion());
        if (request.getDistrict() != null)
            business.setDistrict(request.getDistrict());
        if (request.getWard() != null)
            business.setWard(request.getWard());
        if (request.getStreet() != null)
            business.setStreet(request.getStreet());
        if (request.getLatitude() != null)
            business.setLatitude(request.getLatitude());
        if (request.getLongitude() != null)
            business.setLongitude(request.getLongitude());

        business = businessRepository.save(business);
        return mapToBusinessResponse(business, ownerId);
    }

    /**
     * Get My Products (for business owner)
     */
    public PagedResponse<ProductResponse> getMyProducts(UUID ownerId, int page, int size) {
        Business business = businessRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.findByBusinessIdAndIsActiveTrue(business.getId(), pageable);

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

    /**
     * Get My Orders (incoming orders for business). Optional status filter.
     */
    public PagedResponse<OrderResponse> getMyOrders(UUID ownerId, int page, int size, OrderStatus status) {
        Business business = businessRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = status != null
                ? orderRepository.findByBusinessIdAndStatusOrderByCreatedAtDesc(business.getId(), status, pageable)
                : orderRepository.findByBusinessIdOrderByCreatedAtDesc(business.getId(), pageable);

        return PagedResponse.<OrderResponse>builder()
                .content(orders.getContent().stream()
                        .map(this::mapToOrderResponse)
                        .collect(Collectors.toList()))
                .page(orders.getNumber())
                .size(orders.getSize())
                .totalElements(orders.getTotalElements())
                .totalPages(orders.getTotalPages())
                .last(orders.isLast())
                .first(orders.isFirst())
                .build();
    }

    /**
     * Search businesses
     */
    public PagedResponse<BusinessResponse> searchBusinesses(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Business> businesses = businessRepository.searchBusinesses(query, pageable);
        return buildPagedResponse(businesses);
    }

    /**
     * Get businesses by category
     */
    public PagedResponse<BusinessResponse> getBusinessesByCategory(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Business> businesses = businessRepository.findActiveByCategory(category, pageable);
        return buildPagedResponse(businesses);
    }

    /**
     * Get businesses by region
     */
    public PagedResponse<BusinessResponse> getBusinessesByRegion(String region, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Business> businesses = businessRepository.findActiveByRegion(region, pageable);
        return buildPagedResponse(businesses);
    }

    /**
     * Get all active businesses
     */
    public PagedResponse<BusinessResponse> getAllBusinesses(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Business> businesses = businessRepository.findByStatus(BusinessStatus.ACTIVE, pageable);
        return buildPagedResponse(businesses);
    }

    // Helper methods

    private PagedResponse<BusinessResponse> buildPagedResponse(Page<Business> businesses) {
        return PagedResponse.<BusinessResponse>builder()
                .content(businesses.getContent().stream()
                        .map(b -> mapToBusinessResponse(b, null))
                        .collect(Collectors.toList()))
                .page(businesses.getNumber())
                .size(businesses.getSize())
                .totalElements(businesses.getTotalElements())
                .totalPages(businesses.getTotalPages())
                .last(businesses.isLast())
                .first(businesses.isFirst())
                .build();
    }

    private BusinessResponse mapToBusinessResponse(Business business, UUID currentUserId) {
        Boolean isFollowing = null;
        if (currentUserId != null) {
            isFollowing = businessFollowRepository.existsByUserIdAndBusinessId(currentUserId, business.getId());
        }
        return BusinessResponse.builder()
                .id(business.getId())
                .name(business.getName())
                .description(business.getDescription())
                .category(business.getCategory())
                .logo(business.getLogo())
                .coverImage(business.getCoverImage())
                .status(business.getStatus())
                .isVerified(business.getIsVerified())
                .region(business.getRegion())
                .district(business.getDistrict())
                .ward(business.getWard())
                .street(business.getStreet())
                .latitude(business.getLatitude())
                .longitude(business.getLongitude())
                .owner(PostResponse.UserSummary.builder()
                        .id(business.getOwner().getId())
                        .name(business.getOwner().getName())
                        .profilePic(business.getOwner().getProfilePic())
                        .build())
                .agentId(business.getAgent() != null ? business.getAgent().getId() : null)
                .agentName(business.getAgent() != null ? business.getAgent().getUser().getName() : null)
                .agentCode(business.getAgent() != null ? business.getAgent().getAgentCode() : null)
                .productsCount(business.getProductsCount())
                .followersCount(business.getFollowersCount())
                .rating(business.getRating())
                .reviewsCount(business.getReviewsCount())
                .isFollowing(isFollowing)
                .createdAt(business.getCreatedAt())
                .build();
    }

    private ProductResponse mapToProductResponse(Product product) {
        // Fetch product images
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        Business business = product.getBusiness();

        // Get thumbnail - use product.thumbnail if set, otherwise use first image
        String thumbnail = product.getThumbnail();
        if (thumbnail == null && !images.isEmpty()) {
            thumbnail = images.get(0).getUrl();
        }

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .compareAtPrice(product.getCompareAtPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .isActive(product.getIsActive())
                .thumbnail(thumbnail)
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
                .ordersCount(product.getOrdersCount())
                .rating(product.getRating())
                .reviewsCount(product.getReviewsCount())
                .createdAt(product.getCreatedAt())
                .build();
    }

    private OrderResponse mapToOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .deliveryFee(order.getDeliveryFee())
                .discount(order.getDiscount())
                .total(order.getTotal())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryPhone(order.getDeliveryPhone())
                .deliveryName(order.getDeliveryName())
                .customerNotes(order.getCustomerNotes())
                .isPaid(order.getIsPaid())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
