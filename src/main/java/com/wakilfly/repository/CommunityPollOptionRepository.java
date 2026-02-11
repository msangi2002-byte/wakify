package com.wakilfly.repository;

import com.wakilfly.model.CommunityPollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommunityPollOptionRepository extends JpaRepository<CommunityPollOption, UUID> {
}
