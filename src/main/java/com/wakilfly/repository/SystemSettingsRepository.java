package com.wakilfly.repository;

import com.wakilfly.model.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSetting, UUID> {

    Optional<SystemSetting> findByKey(String key);
}
