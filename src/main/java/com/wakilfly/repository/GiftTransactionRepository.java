package com.wakilfly.repository;

import com.wakilfly.model.GiftTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface GiftTransactionRepository extends JpaRepository<GiftTransaction, UUID> {

    Page<GiftTransaction> findBySenderIdOrderByCreatedAtDesc(UUID senderId, Pageable pageable);

    Page<GiftTransaction> findByReceiverIdOrderByCreatedAtDesc(UUID receiverId, Pageable pageable);

    Page<GiftTransaction> findByLiveStreamIdOrderByCreatedAtDesc(UUID liveStreamId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(gt.totalValue), 0) FROM GiftTransaction gt WHERE gt.receiver.id = :userId")
    BigDecimal sumGiftsReceivedByUser(@Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(gt.totalValue), 0) FROM GiftTransaction gt WHERE gt.liveStream.id = :liveStreamId")
    BigDecimal sumGiftsForLiveStream(@Param("liveStreamId") UUID liveStreamId);
}
