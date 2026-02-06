package com.wakilfly.repository;

import com.wakilfly.model.Payment;
import com.wakilfly.model.PaymentStatus;
import com.wakilfly.model.PaymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Page<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByExternalReference(String externalReference);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    Page<Payment> findByTypeAndStatus(PaymentType type, PaymentStatus status, Pageable pageable);
}
