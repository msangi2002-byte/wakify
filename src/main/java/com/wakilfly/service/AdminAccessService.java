package com.wakilfly.service;

import com.wakilfly.model.AdminArea;
import com.wakilfly.model.Role;
import com.wakilfly.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

/**
 * RBAC: checks if an admin user can access a given admin area.
 * Role definitions (and areas) are stored in DB (AdminRoleDefinition).
 * When user.role != ADMIN, access denied. When user.adminRoleCode is null, treated as SUPER_ADMIN.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminAccessService {

    private final AdminRoleDefinitionService roleDefinitionService;

    /**
     * Resolve effective admin role code: null or missing = SUPER_ADMIN.
     */
    public String effectiveRoleCode(User user) {
        if (user == null || user.getRole() != Role.ADMIN) {
            return null;
        }
        String code = user.getAdminRoleCode();
        return code != null && !code.isBlank() ? code : "SUPER_ADMIN";
    }

    /**
     * Returns true if the admin user is allowed to access the given area.
     */
    public boolean canAccess(User admin, AdminArea area) {
        String roleCode = effectiveRoleCode(admin);
        if (roleCode == null) return false;
        Set<AdminArea> allowed = roleDefinitionService.getAllowedAreas(roleCode);
        return allowed.contains(area);
    }

    /**
     * Throws AccessDeniedException if the current admin cannot access the area.
     */
    public void requireAccess(User admin, AdminArea area) {
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        if (!canAccess(admin, area)) {
            log.warn("Admin {} (role={}) denied access to {}", admin.getId(), effectiveRoleCode(admin), area);
            throw new AccessDeniedException("You do not have permission to access this section");
        }
    }
}
