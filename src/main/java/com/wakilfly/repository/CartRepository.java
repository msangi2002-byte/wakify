package com.wakilfly.repository;

import com.wakilfly.model.Cart;
import com.wakilfly.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByUser(User user);

    Optional<Cart> findByUserId(UUID userId);
}
