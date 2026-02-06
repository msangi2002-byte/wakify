package com.wakilfly.repository;

import com.wakilfly.model.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, UUID> {

    Optional<UserWallet> findByUserId(UUID userId);
}
