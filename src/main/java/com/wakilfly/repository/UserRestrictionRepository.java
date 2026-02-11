package com.wakilfly.repository;

import com.wakilfly.model.UserRestriction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRestrictionRepository extends JpaRepository<UserRestriction, UUID> {

    Optional<UserRestriction> findByRestricterIdAndRestrictedId(UUID restricterId, UUID restrictedId);

    boolean existsByRestricterIdAndRestrictedId(UUID restricterId, UUID restrictedId);

    void deleteByRestricterIdAndRestrictedId(UUID restricterId, UUID restrictedId);

    Page<UserRestriction> findByRestricterIdOrderByCreatedAtDesc(UUID restricterId, Pageable pageable);

    @Query("SELECT ur.restricted.id FROM UserRestriction ur WHERE ur.restricter.id = :restricterId")
    java.util.List<UUID> findRestrictedUserIdsByRestricterId(@Param("restricterId") UUID restricterId);
}
