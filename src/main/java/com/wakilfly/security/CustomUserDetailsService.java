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

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrPhone) throws UsernameNotFoundException {
        User user = userRepository.findByEmailOrPhone(usernameOrPhone)
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
        return userRepository.findByEmailOrPhone(usernameOrPhoneOrId.trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
