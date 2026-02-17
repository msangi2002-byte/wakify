package com.wakilfly.service;

import com.wakilfly.model.AdminArea;
import com.wakilfly.model.AdminRole;
import com.wakilfly.model.Role;
import com.wakilfly.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.Set;

/**
 * RBAC: checks if an admin user can access a given admin area.
 * When user.role != ADMIN, access is denied. When user.adminRole is null, treated as SUPER_ADMIN.
 */
@Service
@Slf4j
public class AdminAccessService {

    private static final Set<AdminArea> SUPER_ONLY = EnumSet.of(
            AdminArea.MAP,
            AdminArea.EXPORT_USERS,
            AdminArea.EXPORT_BUSINESSES,
            AdminArea.AUDIT_LOGS,
            AdminArea.SETTINGS,
            AdminArea.AGENT_PACKAGES,
            AdminArea.IMPERSONATE
    );

    private static final Set<AdminArea> MODERATOR_AREAS = EnumSet.of(
            AdminArea.DASHBOARD,
            AdminArea.REPORTS,
            AdminArea.PROMOTIONS
    );

    private static final Set<AdminArea> SUPPORT_AREAS = EnumSet.of(
            AdminArea.DASHBOARD,
            AdminArea.USERS,
            AdminArea.ORDERS,
            AdminArea.AGENTS,
            AdminArea.BUSINESSES,
            AdminArea.PRODUCTS
    );

    private static final Set<AdminArea> FINANCE_AREAS = EnumSet.of(
            AdminArea.DASHBOARD,
            AdminArea.DASHBOARD_CHARTS,
            AdminArea.PAYMENTS,
            AdminArea.WITHDRAWALS,
            AdminArea.USER_WITHDRAWALS,
            AdminArea.TRANSACTION_REPORTS,
            AdminArea.ANALYTICS
    );

    /**
     * Resolve effective admin role: null or missing = SUPER_ADMIN.
     */
    public AdminRole effectiveRole(User user) {
        if (user == null || user.getRole() != Role.ADMIN) {
            return null;
        }
        return user.getAdminRole() != null ? user.getAdminRole() : AdminRole.SUPER_ADMIN;
    }

    /**
     * Returns true if the admin user is allowed to access the given area.
     */
    public boolean canAccess(User admin, AdminArea area) {
        AdminRole role = effectiveRole(admin);
        if (role == null) {
            return false;
        }
        if (role == AdminRole.SUPER_ADMIN) {
            return true;
        }
        if (SUPER_ONLY.contains(area)) {
            return false;
        }
        switch (role) {
            case MODERATOR:
                return MODERATOR_AREAS.contains(area);
            case SUPPORT_AGENT:
                return SUPPORT_AREAS.contains(area);
            case FINANCE_MANAGER:
                return FINANCE_AREAS.contains(area);
            default:
                return false;
        }
    }

    /**
     * Throws AccessDeniedException if the current admin cannot access the area.
     */
    public void requireAccess(User admin, AdminArea area) {
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        if (!canAccess(admin, area)) {
            log.warn("Admin {} (role={}) denied access to {}", admin.getId(), effectiveRole(admin), area);
            throw new AccessDeniedException("You do not have permission to access this section");
        }
    }
}
