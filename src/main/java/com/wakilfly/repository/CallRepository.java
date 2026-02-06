package com.wakilfly.repository;

import com.wakilfly.model.Call;
import com.wakilfly.model.CallStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CallRepository extends JpaRepository<Call, UUID> {

    @Query("SELECT c FROM Call c WHERE (c.caller.id = :userId OR c.receiver.id = :userId) ORDER BY c.createdAt DESC")
    Page<Call> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT c FROM Call c WHERE (c.caller.id = :userId OR c.receiver.id = :userId) AND c.status = :status")
    List<Call> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") CallStatus status);

    Optional<Call> findByRoomId(String roomId);

    @Query("SELECT c FROM Call c WHERE c.receiver.id = :userId AND c.status = 'RINGING'")
    List<Call> findIncomingCalls(@Param("userId") UUID userId);
}
