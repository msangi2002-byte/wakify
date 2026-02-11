package com.wakilfly.repository;

import com.wakilfly.model.AuthEvent;
import com.wakilfly.model.AuthEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AuthEventRepository extends JpaRepository<AuthEvent, UUID> {

    Page<AuthEvent> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<AuthEvent> findByUserIdAndEventTypeInOrderByCreatedAtDesc(UUID userId, AuthEventType[] types, Pageable pageable);

    long countByIpAddressAndCreatedAtAfter(String ipAddress, LocalDateTime after);

    long countByIdentifierAndSuccessFalseAndCreatedAtAfter(String identifier, LocalDateTime after);
}
