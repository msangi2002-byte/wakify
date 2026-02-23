package com.wakilfly.repository;

import com.wakilfly.model.OrderTrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderTrackingEventRepository extends JpaRepository<OrderTrackingEvent, UUID> {

    List<OrderTrackingEvent> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
