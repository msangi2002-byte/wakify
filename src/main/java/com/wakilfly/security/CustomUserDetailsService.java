package com.wakilfly.security;

import com.wakilfly.model.User;
import com.wakilfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Normalize phone number to consistent format (255XXXXXXXXX)
     * Handles formats: +255..., 255..., 0...
     */
    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        // Check if it looks like an email (contains @)
        if (phone.contains("@")) {
            return phone.trim(); // Don't normalize emails
        }
        // Remove all non-digit characters
        String digits = phone.replaceAll("[^0-9]", "");
        
        // Convert to international format (255XXXXXXXXX)
        if (digits.startsWith("0") && digits.length() > 1) {
            return "255" + digits.substring(1);
        } else if (digits.startsWith("255")) {
            return digits;
        } else if (digits.length() >= 9) {
            // Assume it's a local number without country code
            return "255" + digits;
        }
        return digits;
    }

    /**
     * Find user by trying original input first, then normalized versions
     * This ensures existing users (with various phone formats) can still login
     */
    private Optional<User> findUserByEmailOrPhone(String input) {
        String trimmed = input.trim();
        
        // If it's an email, try exact match only
        if (trimmed.contains("@")) {
            return userRepository.findByEmailOrPhone(trimmed);
        }
        
        // For phone numbers, try multiple variations:
        // 1. Original input (e.g., "+255787654321", "255787654321", "0787654321")
        Optional<User> user = userRepository.findByEmailOrPhone(trimmed);
        if (user.isPresent()) {
            return user;
        }
        
        // 2. Normalized version (e.g., "255787654321")
        String normalized = normalizePhone(trimmed);
        if (!normalized.equals(trimmed)) {
            user = userRepository.findByEmailOrPhone(normalized);
            if (user.isPresent()) {
                return user;
            }
        }
        
        // 3. Try with + prefix if normalized doesn't have it
        if (!normalized.startsWith("+") && normalized.startsWith("255")) {
            user = userRepository.findByEmailOrPhone("+" + normalized);
            if (user.isPresent()) {
                return user;
            }
        }
        
        // 4. Try with 0 prefix (local format)
        if (normalized.startsWith("255") && normalized.length() > 3) {
            String localFormat = "0" + normalized.substring(3);
            user = userRepository.findByEmailOrPhone(localFormat);
            if (user.isPresent()) {
                return user;
            }
        }
        
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrPhone) throws UsernameNotFoundException {
        User user = findUserByEmailOrPhone(usernameOrPhone)
                .orElseThrow(
                        () -> new UsernameNotFoundException("User not found with email or phone: " + usernameOrPhone));

        // Stable username for Spring Security: phone, or email, or id (never null)
        String username = user.getPhone() != null && !user.getPhone().isBlank()
                ? user.getPhone()
                : (user.getEmail() != null && !user.getEmail().isBlank()
                        ? user.getEmail()
                        : user.getId().toString());

        return new org.springframework.security.core.userdetails.User(
                username,
                user.getPassword() != null ? user.getPassword() : "",
                Boolean.TRUE.equals(user.getIsActive()),
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }

    @Transactional(readOnly = true)
    public User loadUserEntityByUsername(String usernameOrPhoneOrId) {
        if (usernameOrPhoneOrId == null || usernameOrPhoneOrId.isBlank()) {
            throw new UsernameNotFoundException("User not found");
        }
        // Try by UUID first (when username was set to id.toString() for users with no phone/email)
        try {
            UUID id = UUID.fromString(usernameOrPhoneOrId.trim());
            return userRepository.findById(id)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        } catch (IllegalArgumentException ignored) {
            // Not a UUID, lookup by email or phone
        }
        return findUserByEmailOrPhone(usernameOrPhoneOrId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
