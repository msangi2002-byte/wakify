package com.wakilfly.repository;

import com.wakilfly.model.Friendship;
import com.wakilfly.model.FriendshipStatus;
import com.wakilfly.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    // Check existing friendship regardless of who asked who
    @Query("SELECT f FROM Friendship f WHERE (f.requester = :user1 AND f.addressee = :user2) OR (f.requester = :user2 AND f.addressee = :user1)")
    Optional<Friendship> findFriendshipBetween(@Param("user1") User user1, @Param("user2") User user2);

    // Get Pending Requests sent TO a user (Incoming)
    @Query("SELECT f FROM Friendship f WHERE f.addressee.id = :userId AND f.status = :status")
    Page<Friendship> findIncomingRequests(@Param("userId") UUID userId, @Param("status") FriendshipStatus status,
            Pageable pageable);

    // Get Pending Requests sent BY a user (Outgoing)
    @Query("SELECT f FROM Friendship f WHERE f.requester.id = :userId AND f.status = :status")
    Page<Friendship> findOutgoingRequests(@Param("userId") UUID userId, @Param("status") FriendshipStatus status,
            Pageable pageable);

    // Get All Friends (Accepted only) needed for logic
    @Query("SELECT f FROM Friendship f WHERE (f.requester.id = :userId OR f.addressee.id = :userId) AND f.status = 'ACCEPTED'")
    Page<Friendship> findAllFriends(@Param("userId") UUID userId, Pageable pageable);

    // Count friends
    @Query("SELECT COUNT(f) FROM Friendship f WHERE (f.requester.id = :userId OR f.addressee.id = :userId) AND f.status = 'ACCEPTED'")
    long countFriends(@Param("userId") UUID userId);
}
