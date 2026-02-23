package com.wakilfly.service;

import com.wakilfly.dto.request.AddTrackingEventRequest;
import com.wakilfly.dto.response.OrderTrackingEventResponse;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.*;
import com.wakilfly.repository.OrderRepository;
import com.wakilfly.repository.OrderTrackingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderTrackingService {

    private final OrderTrackingEventRepository trackingEventRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public OrderTrackingEventResponse addEvent(UUID orderId, UUID sellerUserId, AddTrackingEventRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        if (!order.getBusiness().getOwner().getId().equals(sellerUserId)) {
            throw new BadRequestException("Not your business order");
        }
        OrderTrackingEvent event = OrderTrackingEvent.builder()
                .order(order)
                .eventType(request.getEventType())
                .note(request.getNote())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
        event = trackingEventRepository.save(event);

        // Sync order status and timestamps for key events
        switch (request.getEventType()) {
            case AT_STORE:
            case PACKAGING:
                if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PENDING) {
                    order.setStatus(OrderStatus.PROCESSING);
                    orderRepository.save(order);
                }
                break;
            case SHIPPED:
                order.setStatus(OrderStatus.SHIPPED);
                order.setShippedAt(LocalDateTime.now());
                orderRepository.save(order);
                break;
            case DELIVERED:
                order.setStatus(OrderStatus.DELIVERED);
                order.setDeliveredAt(LocalDateTime.now());
                orderRepository.save(order);
                break;
            case IN_TRANSIT:
                if (order.getStatus() != OrderStatus.DELIVERED) {
                    order.setStatus(OrderStatus.SHIPPED);
                    if (order.getShippedAt() == null) order.setShippedAt(LocalDateTime.now());
                    orderRepository.save(order);
                }
                break;
            default:
                break;
        }
        log.info("Tracking event {} added for order {} by seller {}", event.getEventType(), orderId, sellerUserId);
        return mapToResponse(event);
    }

    public List<OrderTrackingEventResponse> getEventsForOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        boolean isBuyer = order.getBuyer().getId().equals(userId);
        boolean isSeller = order.getBusiness().getOwner().getId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new BadRequestException("Not allowed to view this order's tracking");
        }
        List<OrderTrackingEvent> events = trackingEventRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
        return events.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private OrderTrackingEventResponse mapToResponse(OrderTrackingEvent e) {
        return OrderTrackingEventResponse.builder()
                .id(e.getId())
                .eventType(e.getEventType())
                .note(e.getNote())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
