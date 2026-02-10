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

    /** Find user by email OR phone (same input for both: login with email or phone). */
    @Query("SELECT u FROM User u WHERE (u.email IS NOT NULL AND u.email = :input) OR u.phone = :input")
    Optional<User> findByEmailOrPhone(@Param("input") String emailOrPhone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR (u.phone IS NOT NULL AND u.phone LIKE CONCAT('%', :query, '%'))) ORDER BY u.name ASC")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);

    /** Suggested users: not self, not already following, optionally same region/country, ordered by name. */
    @Query("SELECT u FROM User u WHERE u.id <> :currentUserId AND u.isActive = true " +
            "AND u NOT IN (SELECT f FROM User me JOIN me.following f WHERE me.id = :currentUserId) " +
            "AND (:region IS NULL OR :region = '' OR u.region = :region) " +
            "AND (:country IS NULL OR :country = '' OR u.country = :country) " +
            "ORDER BY u.name ASC")
    Page<User> findSuggestedUsers(@Param("currentUserId") UUID currentUserId, @Param("region") String region, @Param("country") String country, Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.followers f WHERE f.id = :userId")
    Page<User> findFollowing(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT f FROM User u JOIN u.followers f WHERE u.id = :userId")
    Page<User> findFollowers(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(f) FROM User u JOIN u.followers f WHERE u.id = :userId")
    long countFollowers(@Param("userId") UUID userId);

    @Query("SELECT COUNT(u) FROM User u JOIN u.followers f WHERE f.id = :userId")
    long countFollowing(@Param("userId") UUID userId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM User u JOIN u.following f WHERE u.id = :followerId AND f.id = :followingId")
    boolean isFollowing(@Param("followerId") UUID followerId, @Param("followingId") UUID followingId);

    /** Mutual follows: users I follow AND who follow me back */
    @Query("SELECT f FROM User me JOIN me.following f JOIN me.followers fr WHERE me.id = :userId AND fr.id = f.id ORDER BY f.name ASC")
    Page<User> findMutualFollows(@Param("userId") UUID userId, Pageable pageable);

    // Admin stats methods
    long countByIsActiveTrue();

    long countByCreatedAtAfter(LocalDateTime date);

    Page<User> findByRoleAndIsActive(Role role, Boolean isActive, Pageable pageable);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByIsActive(Boolean isActive, Pageable pageable);

    long countByRole(Role role);
}
