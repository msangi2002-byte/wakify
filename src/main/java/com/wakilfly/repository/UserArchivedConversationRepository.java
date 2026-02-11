package com.wakilfly.repository;

import com.wakilfly.model.UserArchivedConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserArchivedConversationRepository extends JpaRepository<UserArchivedConversation, UUID> {

    Optional<UserArchivedConversation> findByUserIdAndOtherUserId(UUID userId, UUID otherUserId);

    List<UserArchivedConversation> findByUserId(UUID userId);

    boolean existsByUserIdAndOtherUserId(UUID userId, UUID otherUserId);

    void deleteByUserIdAndOtherUserId(UUID userId, UUID otherUserId);
}
