package com.wakilfly.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.model.AdminArea;
import com.wakilfly.model.AdminRoleDefinition;
import com.wakilfly.repository.AdminRoleDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminRoleDefinitionService {

    private static final List<String> BUILTIN_CODES = List.of("SUPER_ADMIN", "MODERATOR", "SUPPORT_AGENT", "FINANCE_MANAGER");

    private final AdminRoleDefinitionRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<AdminRoleDefinition> listAll() {
        return repository.findAllByOrderByIsBuiltinDescDisplayNameAsc();
    }

    public Optional<AdminRoleDefinition> findByCode(String code) {
        return repository.findByCode(code);
    }

    /**
     * Get allowed areas for a role code. SUPER_ADMIN = all areas. Others from DB.
     */
    public Set<AdminArea> getAllowedAreas(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return EnumSet.allOf(AdminArea.class); // SUPER_ADMIN
        }
        if ("SUPER_ADMIN".equalsIgnoreCase(roleCode)) {
            return EnumSet.allOf(AdminArea.class);
        }
        return repository.findByCode(roleCode)
                .map(this::parseAreas)
                .orElse(Set.of());
    }

    private Set<AdminArea> parseAreas(AdminRoleDefinition def) {
        if (def.getAreasJson() == null || def.getAreasJson().isBlank()) {
            return Set.of();
        }
        try {
            List<String> names = objectMapper.readValue(def.getAreasJson(), new TypeReference<>() {});
            return names.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> {
                        try {
                            return AdminArea.valueOf(s);
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("Failed to parse areas JSON for role {}: {}", def.getCode(), e.getMessage());
            return Set.of();
        }
    }

    @Transactional
    public AdminRoleDefinition create(String code, String displayName, List<String> areas) {
        code = code != null ? code.trim().toUpperCase().replaceAll("\\s+", "_") : "";
        if (code.isEmpty()) throw new BadRequestException("Role code is required");
        if (repository.existsByCode(code)) throw new BadRequestException("Role code already exists: " + code);
        if (displayName == null || displayName.isBlank()) throw new BadRequestException("Display name is required");
        AdminRoleDefinition def = AdminRoleDefinition.builder()
                .code(code)
                .displayName(displayName.trim())
                .areasJson(toJson(areas))
                .isBuiltin(false)
                .build();
        def = repository.save(def);
        log.info("Created admin role definition: {} ({})", def.getCode(), def.getDisplayName());
        return def;
    }

    @Transactional
    public AdminRoleDefinition update(java.util.UUID id, String displayName, List<String> areas) {
        AdminRoleDefinition def = repository.findById(id)
                .orElseThrow(() -> new com.wakilfly.exception.ResourceNotFoundException("AdminRoleDefinition", "id", id));
        if (displayName != null && !displayName.isBlank()) def.setDisplayName(displayName.trim());
        if (areas != null) def.setAreasJson(toJson(areas));
        def = repository.save(def);
        log.info("Updated admin role definition: {}", def.getCode());
        return def;
    }

    @Transactional
    public void delete(java.util.UUID id) {
        AdminRoleDefinition def = repository.findById(id)
                .orElseThrow(() -> new com.wakilfly.exception.ResourceNotFoundException("AdminRoleDefinition", "id", id));
        if (Boolean.TRUE.equals(def.getIsBuiltin())) {
            throw new BadRequestException("Cannot delete built-in role: " + def.getCode());
        }
        repository.delete(def);
        log.info("Deleted admin role definition: {}", def.getCode());
    }

    public boolean isValidRoleCode(String code) {
        if (code == null || code.isBlank()) return true;
        return BUILTIN_CODES.contains(code) || repository.existsByCode(code);
    }

    private String toJson(List<String> areas) {
        if (areas == null || areas.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(areas);
        } catch (Exception e) {
            throw new BadRequestException("Invalid areas: " + e.getMessage());
        }
    }

    /** Seed built-in roles if table is empty. */
    @Transactional
    public void seedBuiltinIfEmpty() {
        if (repository.count() > 0) return;
        List<AdminRoleDefinition> builtins = List.of(
                AdminRoleDefinition.builder().code("SUPER_ADMIN").displayName("Super Admin").areasJson("[]").isBuiltin(true).build(),
                AdminRoleDefinition.builder().code("MODERATOR").displayName("Moderator").areasJson("[\"DASHBOARD\",\"REPORTS\",\"PROMOTIONS\"]").isBuiltin(true).build(),
                AdminRoleDefinition.builder().code("SUPPORT_AGENT").displayName("Support").areasJson("[\"DASHBOARD\",\"USERS\",\"ORDERS\",\"AGENTS\",\"BUSINESSES\",\"PRODUCTS\"]").isBuiltin(true).build(),
                AdminRoleDefinition.builder().code("FINANCE_MANAGER").displayName("Finance").areasJson("[\"DASHBOARD\",\"DASHBOARD_CHARTS\",\"PAYMENTS\",\"WITHDRAWALS\",\"USER_WITHDRAWALS\",\"TRANSACTION_REPORTS\",\"ANALYTICS\"]").isBuiltin(true).build()
        );
        repository.saveAll(builtins);
        log.info("Seeded {} built-in admin role definitions", builtins.size());
    }
}
