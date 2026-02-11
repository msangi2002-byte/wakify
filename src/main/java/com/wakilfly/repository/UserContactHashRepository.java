package com.wakilfly.repository;

import com.wakilfly.model.UserContactHash;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserContactHashRepository extends JpaRepository<UserContactHash, UUID> {

    List<UserContactHash> findByUserId(UUID userId);

    /** Check if this user has a given hash (phone or email) in their contacts. */
    boolean existsByUserIdAndContactTypeAndHash(UUID userId, UserContactHash.ContactHashType contactType, String hash);

    /** Get all hashes for a user (for matching candidates). */
    @Query("SELECT h.hash FROM UserContactHash h WHERE h.userId = :userId")
    Set<String> findHashesByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM UserContactHash h WHERE h.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
