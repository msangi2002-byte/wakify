package com.wakilfly.repository;

import com.wakilfly.model.Community;
import com.wakilfly.model.CommunityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommunityRepository extends JpaRepository<Community, UUID> {

    // Search communities by name (for Discover feature)
    @Query("SELECT c FROM Community c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Community> searchByName(@Param("query") String query, Pageable pageable);

    // Find communities by creator (My Communities)
    Page<Community> findByCreatorId(UUID creatorId, Pageable pageable);

    // Find communities by type (Groups vs Channels)
    Page<Community> findByType(CommunityType type, Pageable pageable);
}
