package com.wakilfly.repository;

import com.wakilfly.model.Message;
import com.wakilfly.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    // Get chat history between two users
    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender = :user1 AND m.recipient = :user2) OR " +
            "(m.sender = :user2 AND m.recipient = :user1) " +
            "ORDER BY m.createdAt DESC")
    Page<Message> findConversation(@Param("user1") User user1, @Param("user2") User user2, Pageable pageable);

    // Count unread messages for a user
    long countByRecipientAndIsReadFalse(User recipient);

    // Count unread from specific user
    long countBySenderAndRecipientAndIsReadFalse(User sender, User recipient);

    @Query("SELECT m FROM Message m WHERE m.sender = :user OR m.recipient = :user ORDER BY m.createdAt DESC")
    Page<Message> findRecentForUser(@Param("user") User user, Pageable pageable);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.recipient = :recipient AND m.sender = :sender AND m.isRead = false")
    int markAsReadByRecipientAndSender(@Param("recipient") User recipient, @Param("sender") User sender);
}
