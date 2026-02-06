package com.wakilfly.repository;

import com.wakilfly.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("SELECT c FROM Conversation c WHERE (c.participantOne.id = :userId OR c.participantTwo.id = :userId) ORDER BY c.lastMessageAt DESC")
    Page<Conversation> findUserConversations(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE " +
            "(c.participantOne.id = :userId1 AND c.participantTwo.id = :userId2) OR " +
            "(c.participantOne.id = :userId2 AND c.participantTwo.id = :userId1)")
    Optional<Conversation> findByParticipants(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);
}
