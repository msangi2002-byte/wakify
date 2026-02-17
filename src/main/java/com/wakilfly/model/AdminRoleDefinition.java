package com.wakilfly.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Stores admin role definitions (built-in + custom) with their allowed areas.
 * Built-in codes: SUPER_ADMIN, MODERATOR, SUPPORT_AGENT, FINANCE_MANAGER.
 * Custom roles can be added via API.
 */
@Entity
@Table(name = "admin_role_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRoleDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 64)
    private String code;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /** JSON array of AdminArea names, e.g. ["DASHBOARD","USERS","ORDERS"]. SUPER_ADMIN = all (empty or null means all). */
    @Column(columnDefinition = "TEXT")
    private String areasJson;

    /** Built-in roles (SUPER_ADMIN, MODERATOR, etc.) cannot be deleted. */
    @Column(name = "is_builtin", nullable = false)
    @Builder.Default
    private Boolean isBuiltin = false;
}
