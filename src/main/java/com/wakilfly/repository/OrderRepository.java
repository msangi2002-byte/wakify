package com.wakilfly.repository;

import com.wakilfly.model.Order;
import com.wakilfly.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    // Buyer orders
    Page<Order> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId, Pageable pageable);

    Page<Order> findByBuyerIdAndStatusOrderByCreatedAtDesc(UUID buyerId, OrderStatus status, Pageable pageable);

    // Seller orders
    Page<Order> findByBusinessIdOrderByCreatedAtDesc(UUID businessId, Pageable pageable);

    Page<Order> findByBusinessIdAndStatusOrderByCreatedAtDesc(UUID businessId, OrderStatus status, Pageable pageable);

    // Counts
    @Query("SELECT COUNT(o) FROM Order o WHERE o.buyer.id = :buyerId")
    long countByBuyerId(@Param("buyerId") UUID buyerId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.business.id = :businessId")
    long countByBusinessId(@Param("businessId") UUID businessId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.business.id = :businessId AND o.status = :status")
    long countByBusinessIdAndStatus(@Param("businessId") UUID businessId, @Param("status") OrderStatus status);

    // Stats
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.business.id = :businessId AND o.status = 'DELIVERED'")
    BigDecimal sumTotalSalesByBusinessId(@Param("businessId") UUID businessId);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.business.id = :businessId AND o.status = 'DELIVERED' AND o.deliveredAt >= :startDate")
    BigDecimal sumSalesByBusinessIdSince(@Param("businessId") UUID businessId,
            @Param("startDate") LocalDateTime startDate);

    long countByStatus(OrderStatus status);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT o FROM Order o WHERE (:status IS NULL OR o.status = :status) ORDER BY o.createdAt DESC")
    Page<Order> findAllWithStatusFilter(@Param("status") OrderStatus status, Pageable pageable);

    /** Count distinct buyers (users who have placed at least one order) – for "Online shoppers" segment. */
    @Query("SELECT COUNT(DISTINCT o.buyer.id) FROM Order o")
    long countDistinctBuyers();
}
