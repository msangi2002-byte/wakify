package com.wakilfly.service;

import com.wakilfly.dto.request.CreateDraftOrderRequest;
import com.wakilfly.dto.request.CreateOrderRequest;
import com.wakilfly.dto.request.UpdateDraftOrderRequest;
import com.wakilfly.dto.request.UpdateOrderStatusRequest;
import com.wakilfly.dto.response.OrderResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.model.*;
import com.wakilfly.exception.BadRequestException;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductInquiryRepository productInquiryRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final ProductImageRepository productImageRepository;
    private final InquiryService inquiryService;

    /**
     * Create a new order
     */
    @Transactional
    public OrderResponse createOrder(UUID buyerId, CreateOrderRequest request) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", buyerId));

        // Group items by business
        Map<UUID, List<CreateOrderRequest.OrderItemRequest>> itemsByBusiness = new HashMap<>();

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemRequest.getProductId()));

            if (!product.getIsActive()) {
                throw new BadRequestException("Product '" + product.getName() + "' is not available");
            }

            UUID businessId = product.getBusiness().getId();
            itemsByBusiness.computeIfAbsent(businessId, k -> new ArrayList<>()).add(itemRequest);
        }

        // For simplicity, we only allow ordering from one business at a time
        if (itemsByBusiness.size() > 1) {
            throw new BadRequestException("All products must be from the same business");
        }

        UUID businessId = itemsByBusiness.keySet().iterator().next();
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));

        // Create order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .buyer(buyer)
                .business(business)
                .deliveryName(request.getDeliveryName())
                .deliveryPhone(request.getDeliveryPhone())
                .deliveryAddress(request.getDeliveryAddress())
                .deliveryRegion(request.getDeliveryRegion())
                .deliveryDistrict(request.getDeliveryDistrict())
                .customerNotes(request.getCustomerNotes())
                .subtotal(BigDecimal.ZERO)
                .deliveryFee(BigDecimal.ZERO) // Could be calculated based on region
                .discount(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .status(OrderStatus.PENDING)
                .build();

        // Add order items
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId()).get();

            // Check stock
            if (product.getTrackStock() && product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new BadRequestException("Insufficient stock for product: " + product.getName());
            }

            // Get primary image
            String productImage = null;
            List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
            if (!images.isEmpty()) {
                productImage = images.get(0).getUrl();
            }

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .productImage(productImage)
                    .unitPrice(product.getPrice())
                    .quantity(itemRequest.getQuantity())
                    .build();
            item.calculateTotal();
            order.addItem(item);

            // Reduce stock
            if (product.getTrackStock()) {
                product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());
                productRepository.save(product);
            }

            // Increment orders count
            product.setOrdersCount(product.getOrdersCount() + 1);
            productRepository.save(product);
        }

        // Calculate totals
        order.calculateTotals();
        order = orderRepository.save(order);

        log.info("Order {} created by user {} for business {}", order.getOrderNumber(), buyerId, businessId);

        // TODO: Send notification to business owner
        // TODO: Send order confirmation to buyer

        return mapToOrderResponse(order);
    }

    /**
     * Create draft order from accepted inquiry (Alibaba-style). No stock reduction until buyer confirms.
     */
    @Transactional
    public OrderResponse createDraftOrderFromInquiry(UUID buyerId, CreateDraftOrderRequest request) {
        ProductInquiry inquiry = productInquiryRepository.findById(request.getInquiryId())
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry", "id", request.getInquiryId()));
        if (!inquiry.getBuyer().getId().equals(buyerId)) {
            throw new BadRequestException("Not your inquiry");
        }
        if (inquiry.getStatus() != InquiryStatus.ACCEPTED) {
            throw new BadRequestException("Inquiry must be accepted before creating order");
        }
        if (inquiry.getConvertedOrderId() != null) {
            throw new BadRequestException("Order already created for this inquiry");
        }
        User buyer = inquiry.getBuyer();
        Business business = inquiry.getBusiness();
        Product product = inquiry.getProduct();
        int qty = inquiry.getQuantity() != null ? inquiry.getQuantity() : 1;
        BigDecimal unitPrice = inquiry.getQuotedPrice() != null ? inquiry.getQuotedPrice() : product.getPrice();
        BigDecimal deliveryFee = inquiry.getQuotedDeliveryFee() != null ? inquiry.getQuotedDeliveryFee() : BigDecimal.ZERO;

        String productImage = null;
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        if (!images.isEmpty()) productImage = images.get(0).getUrl();

        BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .buyer(buyer)
                .business(business)
                .deliveryName(request.getDeliveryName())
                .deliveryPhone(request.getDeliveryPhone())
                .deliveryAddress(request.getDeliveryAddress())
                .deliveryRegion(request.getDeliveryRegion())
                .deliveryDistrict(request.getDeliveryDistrict())
                .customerNotes(request.getCustomerNotes())
                .subtotal(itemTotal)
                .deliveryFee(deliveryFee)
                .discount(BigDecimal.ZERO)
                .total(itemTotal.add(deliveryFee))
                .status(OrderStatus.DRAFT)
                .source(OrderSource.INQUIRY)
                .inquiryId(inquiry.getId())
                .build();
        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .productName(product.getName())
                .productImage(productImage)
                .unitPrice(unitPrice)
                .quantity(qty)
                .total(itemTotal)
                .build();
        order.addItem(item);
        order = orderRepository.save(order);

        inquiryService.markConvertedToOrder(inquiry.getId(), order.getId());

        log.info("Draft order {} created from inquiry {} for buyer {}", order.getOrderNumber(), inquiry.getId(), buyerId);
        return mapToOrderResponse(order);
    }

    /**
     * Seller updates draft order (delivery fee, discount, notes). No stock change.
     */
    @Transactional
    public OrderResponse updateDraftOrder(UUID orderId, UUID businessOwnerId, UpdateDraftOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        if (!order.getBusiness().getOwner().getId().equals(businessOwnerId)) {
            throw new BadRequestException("Not your business order");
        }
        if (order.getStatus() != OrderStatus.DRAFT && order.getStatus() != OrderStatus.PENDING_CONFIRMATION) {
            throw new BadRequestException("Only draft orders can be updated");
        }
        if (request.getDeliveryFee() != null) order.setDeliveryFee(request.getDeliveryFee());
        if (request.getDiscount() != null) order.setDiscount(request.getDiscount());
        if (request.getSellerNotes() != null) order.setSellerNotes(request.getSellerNotes());
        order.calculateTotals();
        order = orderRepository.save(order);
        return mapToOrderResponse(order);
    }

    /**
     * Buyer confirms draft order (ready for payment). Reduces stock.
     */
    @Transactional
    public OrderResponse confirmOrderByBuyer(UUID orderId, UUID buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new BadRequestException("Not your order");
        }
        if (order.getStatus() != OrderStatus.DRAFT && order.getStatus() != OrderStatus.PENDING_CONFIRMATION) {
            throw new BadRequestException("Order is not a draft");
        }
        validateStatusTransition(order.getStatus(), OrderStatus.CONFIRMED);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product.getTrackStock() && product.getStockQuantity() < item.getQuantity()) {
                throw new BadRequestException("Insufficient stock for: " + product.getName());
            }
            if (product.getTrackStock()) {
                product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
                productRepository.save(product);
            }
            product.setOrdersCount(product.getOrdersCount() != null ? product.getOrdersCount() + 1 : 1);
            productRepository.save(product);
        }
        order = orderRepository.save(order);
        log.info("Order {} confirmed by buyer {}", order.getOrderNumber(), buyerId);
        return mapToOrderResponse(order);
    }

    /**
     * Get order by ID
     */
    public OrderResponse getOrderById(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Check if user has access to this order
        if (!order.getBuyer().getId().equals(userId) &&
                !order.getBusiness().getOwner().getId().equals(userId)) {
            throw new BadRequestException("You don't have access to this order");
        }

        return mapToOrderResponse(order);
    }

    /**
     * Get buyer's orders
     */
    public PagedResponse<OrderResponse> getBuyerOrders(UUID buyerId, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders;

        if (status != null) {
            orders = orderRepository.findByBuyerIdAndStatusOrderByCreatedAtDesc(buyerId, status, pageable);
        } else {
            orders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId, pageable);
        }

        return buildPagedResponse(orders);
    }

    /**
     * Get all orders (admin)
     */
    public PagedResponse<OrderResponse> getAllOrdersForAdmin(OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = status != null
                ? orderRepository.findAllWithStatusFilter(status, pageable)
                : orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        return buildPagedResponse(orders);
    }

    /**
     * Get order by ID (admin - no ownership check)
     */
    public OrderResponse getOrderByIdForAdmin(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return mapToOrderResponse(order);
    }

    /**
     * Update order status (admin - bypasses business ownership)
     */
    @Transactional
    public OrderResponse updateOrderStatusForAdmin(UUID orderId, UUID adminId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        OrderStatus newStatus = request.getStatus();
        OrderStatus currentStatus = order.getStatus();
        validateStatusTransition(currentStatus, newStatus);

        order.setStatus(newStatus);
        switch (newStatus) {
            case CONFIRMED:
                order.setConfirmedAt(LocalDateTime.now());
                break;
            case SHIPPED:
                order.setShippedAt(LocalDateTime.now());
                if (request.getTrackingNumber() != null) {
                    order.setTrackingNumber(request.getTrackingNumber());
                }
                break;
            case DELIVERED:
            case COMPLETED:
                order.setDeliveredAt(LocalDateTime.now());
                break;
            case CANCELLED:
                order.setCancelledAt(LocalDateTime.now());
                order.setCancellationReason(request.getCancellationReason());
                restoreStock(order);
                break;
            default:
                break;
        }
        if (request.getSellerNotes() != null) {
            order.setSellerNotes(request.getSellerNotes());
        }
        order = orderRepository.save(order);
        log.info("Admin {} updated order {} to {}", adminId, order.getOrderNumber(), newStatus);
        return mapToOrderResponse(order);
    }

    /**
     * Get business orders (for seller)
     */
    public PagedResponse<OrderResponse> getBusinessOrders(UUID businessId, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders;

        if (status != null) {
            orders = orderRepository.findByBusinessIdAndStatusOrderByCreatedAtDesc(businessId, status, pageable);
        } else {
            orders = orderRepository.findByBusinessIdOrderByCreatedAtDesc(businessId, pageable);
        }

        return buildPagedResponse(orders);
    }

    /**
     * Update order status (by seller)
     */
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, UUID businessOwnerId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Verify business owner
        if (!order.getBusiness().getOwner().getId().equals(businessOwnerId)) {
            throw new BadRequestException("You can only update orders for your business");
        }

        OrderStatus newStatus = request.getStatus();
        OrderStatus currentStatus = order.getStatus();

        // Only buyer can confirm draft/pending_confirmation (via confirmOrderByBuyer)
        if ((currentStatus == OrderStatus.DRAFT || currentStatus == OrderStatus.PENDING_CONFIRMATION) && newStatus == OrderStatus.CONFIRMED) {
            throw new BadRequestException("Buyer must confirm the order from their side");
        }

        // Validate status transition
        validateStatusTransition(currentStatus, newStatus);

        order.setStatus(newStatus);

        switch (newStatus) {
            case CONFIRMED:
                order.setConfirmedAt(LocalDateTime.now());
                break;
            case PROCESSING:
                // Order is being prepared
                break;
            case SHIPPED:
                order.setShippedAt(LocalDateTime.now());
                if (request.getTrackingNumber() != null) {
                    order.setTrackingNumber(request.getTrackingNumber());
                }
                break;
            case DELIVERED:
            case COMPLETED:
                order.setDeliveredAt(LocalDateTime.now());
                break;
            case CANCELLED:
                order.setCancelledAt(LocalDateTime.now());
                order.setCancellationReason(request.getCancellationReason());
                // Restore stock
                restoreStock(order);
                break;
            case REFUNDED:
                // Handle refund logic
                break;
            case PENDING:
            case DRAFT:
            case PENDING_CONFIRMATION:
                // No timestamp/stock change
                break;
        }

        if (request.getSellerNotes() != null) {
            order.setSellerNotes(request.getSellerNotes());
        }

        order = orderRepository.save(order);

        // TODO: Send notification to buyer about status change

        log.info("Order {} status updated to {} by business owner {}",
                order.getOrderNumber(), newStatus, businessOwnerId);

        return mapToOrderResponse(order);
    }

    /**
     * Cancel order (by buyer, only if pending)
     */
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID buyerId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Verify buyer
        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new BadRequestException("You can only cancel your own orders");
        }

        // Can cancel pending, draft, or pending_confirmation orders (before payment)
        Set<OrderStatus> cancellable = Set.of(OrderStatus.PENDING, OrderStatus.DRAFT, OrderStatus.PENDING_CONFIRMATION);
        if (!cancellable.contains(order.getStatus())) {
            throw new BadRequestException("Cannot cancel order. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason);

        // Restore stock
        restoreStock(order);

        order = orderRepository.save(order);

        log.info("Order {} cancelled by buyer {}", order.getOrderNumber(), buyerId);

        return mapToOrderResponse(order);
    }

    // Helper methods

    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        Map<OrderStatus, Set<OrderStatus>> validTransitions = new HashMap<>();
        validTransitions.put(OrderStatus.DRAFT, Set.of(OrderStatus.PENDING_CONFIRMATION, OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        validTransitions.put(OrderStatus.PENDING_CONFIRMATION, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        validTransitions.put(OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        validTransitions.put(OrderStatus.CONFIRMED,
                Set.of(OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        validTransitions.put(OrderStatus.PROCESSING, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        validTransitions.put(OrderStatus.SHIPPED,
                Set.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED, OrderStatus.CANCELLED));
        validTransitions.put(OrderStatus.DELIVERED, Set.of(OrderStatus.COMPLETED, OrderStatus.REFUNDED));
        validTransitions.put(OrderStatus.COMPLETED, Set.of(OrderStatus.REFUNDED));
        validTransitions.put(OrderStatus.CANCELLED, Set.of());
        validTransitions.put(OrderStatus.REFUNDED, Set.of());

        if (!validTransitions.getOrDefault(from, Set.of()).contains(to)) {
            throw new BadRequestException("Invalid status transition from " + from + " to " + to);
        }
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product.getTrackStock()) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }
    }

    private String generateOrderNumber() {
        return "ORD" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
    }

    private PagedResponse<OrderResponse> buildPagedResponse(Page<Order> orders) {
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

    private OrderResponse mapToOrderResponse(Order order) {
        User buyer = order.getBuyer();
        Business business = order.getBusiness();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .source(order.getSource() != null ? order.getSource() : OrderSource.CART)
                .inquiryId(order.getInquiryId())
                .buyer(OrderResponse.UserSummary.builder()
                        .id(buyer.getId())
                        .name(buyer.getName())
                        .phone(buyer.getPhone())
                        .profilePic(buyer.getProfilePic())
                        .build())
                .business(OrderResponse.BusinessSummary.builder()
                        .id(business.getId())
                        .name(business.getName())
                        .logo(business.getLogo())
                        .phone(business.getPhone())
                        .email(business.getEmail())
                        .website(business.getWebsite())
                        .region(business.getRegion())
                        .district(business.getDistrict())
                        .build())
                .items(order.getItems().stream()
                        .map(item -> OrderResponse.OrderItemResponse.builder()
                                .id(item.getId())
                                .productId(item.getProduct().getId())
                                .productName(item.getProductName())
                                .productImage(item.getProductImage())
                                .unitPrice(item.getUnitPrice())
                                .quantity(item.getQuantity())
                                .total(item.getTotal())
                                .build())
                        .collect(Collectors.toList()))
                .totalItems(order.getItems().size())
                .subtotal(order.getSubtotal())
                .deliveryFee(order.getDeliveryFee())
                .discount(order.getDiscount())
                .total(order.getTotal())
                .isPaid(order.getIsPaid())
                .paidAt(order.getPaidAt())
                .paymentMethod(order.getPaymentMethod())
                .deliveryName(order.getDeliveryName())
                .deliveryPhone(order.getDeliveryPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryRegion(order.getDeliveryRegion())
                .deliveryDistrict(order.getDeliveryDistrict())
                .customerNotes(order.getCustomerNotes())
                .sellerNotes(order.getSellerNotes())
                .trackingNumber(order.getTrackingNumber())
                .createdAt(order.getCreatedAt())
                .confirmedAt(order.getConfirmedAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .cancellationReason(order.getCancellationReason())
                .build();
    }
}
