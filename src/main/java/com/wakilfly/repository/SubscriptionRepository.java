package com.wakilfly.repository;

import com.wakilfly.entity.Subscription;
import com.wakilfly.entity.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByBusinessId(UUID businessId);

    Page<Subscription> findByStatus(SubscriptionStatus status, Pageable pageable);

    // Find active subscriptions
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate > :now")
    List<Subscription> findAllActiveSubscriptions(@Param("now") LocalDateTime now);

    // Find subscriptions expiring soon (for reminders)
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate BETWEEN :now AND :targetDate AND s.reminderSent7Days = false")
    List<Subscription> findSubscriptionsExpiringIn7Days(@Param("now") LocalDateTime now,
            @Param("targetDate") LocalDateTime targetDate);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate BETWEEN :now AND :targetDate AND s.reminderSent3Days = false")
    List<Subscription> findSubscriptionsExpiringIn3Days(@Param("now") LocalDateTime now,
            @Param("targetDate") LocalDateTime targetDate);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate BETWEEN :now AND :targetDate AND s.reminderSent1Day = false")
    List<Subscription> findSubscriptionsExpiringIn1Day(@Param("now") LocalDateTime now,
            @Param("targetDate") LocalDateTime targetDate);

    // Find expired subscriptions
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate < :now")
    List<Subscription> findExpiredSubscriptions(@Param("now") LocalDateTime now);

    // Stats
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = 'ACTIVE'")
    long countActiveSubscriptions();

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = 'EXPIRED'")
    long countExpiredSubscriptions();
}
