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

        return new org.springframework.security.core.userdetails.User(
                user.getPhone(), // Using phone as username
                user.getPassword(),
                user.getIsActive(),
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }

    @Transactional(readOnly = true)
    public User loadUserEntityByUsername(String usernameOrPhone) {
        return userRepository.findByEmailOrPhone(usernameOrPhone)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
