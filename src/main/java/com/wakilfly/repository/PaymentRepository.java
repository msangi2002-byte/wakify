package com.wakilfly.repository;

import com.wakilfly.model.Payment;
import com.wakilfly.model.PaymentStatus;
import com.wakilfly.model.PaymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Page<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByExternalReference(String externalReference);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    // List version for scheduled job
    List<Payment> findByStatus(PaymentStatus status);

    Page<Payment> findByTypeAndStatus(PaymentType type, PaymentStatus status, Pageable pageable);

    Optional<Payment> findFirstByUserIdAndTypeAndStatusAndRelatedEntityTypeOrderByCreatedAtDesc(
            UUID userId, PaymentType type, PaymentStatus status, String relatedEntityType);

    Optional<Payment> findByRelatedEntityTypeAndRelatedEntityId(String relatedEntityType, UUID relatedEntityId);

    // Revenue calculation methods
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status")
    BigDecimal sumByStatus(@Param("status") PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status AND p.createdAt >= :date")
    BigDecimal sumByStatusAndDateAfter(@Param("status") PaymentStatus status, @Param("date") LocalDateTime date);

    // Admin: Get all payments with filters
    @Query("SELECT p FROM Payment p WHERE " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:type IS NULL OR p.type = :type) AND " +
            "(:userId IS NULL OR p.user.id = :userId) AND " +
            "(:startDate IS NULL OR p.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR p.createdAt <= :endDate) " +
            "ORDER BY p.createdAt DESC")
    Page<Payment> findAllWithFilters(
            @Param("status") PaymentStatus status,
            @Param("type") PaymentType type,
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
