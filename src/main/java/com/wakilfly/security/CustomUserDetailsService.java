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

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrPhone) throws UsernameNotFoundException {
        // Normalize phone number if it's a phone (not an email)
        String normalizedInput = normalizePhone(usernameOrPhone.trim());
        
        User user = userRepository.findByEmailOrPhone(normalizedInput)
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
        // Normalize phone number if it's a phone (not an email)
        String normalizedInput = normalizePhone(usernameOrPhoneOrId.trim());
        return userRepository.findByEmailOrPhone(normalizedInput)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
