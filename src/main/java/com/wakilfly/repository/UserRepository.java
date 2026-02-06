package com.wakilfly.repository;

import com.wakilfly.model.Role;
import com.wakilfly.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmailOrPhone(String email, String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.followers f WHERE f.id = :userId")
    Page<User> findFollowing(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT f FROM User u JOIN u.followers f WHERE u.id = :userId")
    Page<User> findFollowers(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(f) FROM User u JOIN u.followers f WHERE u.id = :userId")
    long countFollowers(@Param("userId") UUID userId);

    @Query("SELECT COUNT(u) FROM User u JOIN u.followers f WHERE f.id = :userId")
    long countFollowing(@Param("userId") UUID userId);

    // Admin stats methods
    long countByIsActiveTrue();

    long countByCreatedAtAfter(LocalDateTime date);

    Page<User> findByRoleAndIsActive(Role role, Boolean isActive, Pageable pageable);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByIsActive(Boolean isActive, Pageable pageable);

    long countByRole(Role role);
}
