package com.wakilfly.repository;

import com.wakilfly.model.UserCashWithdrawal;
import com.wakilfly.model.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserCashWithdrawalRepository extends JpaRepository<UserCashWithdrawal, UUID> {

    Page<UserCashWithdrawal> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<UserCashWithdrawal> findByStatusOrderByCreatedAtDesc(WithdrawalStatus status, Pageable pageable);

    boolean existsByUserIdAndStatus(UUID userId, WithdrawalStatus status);
}
