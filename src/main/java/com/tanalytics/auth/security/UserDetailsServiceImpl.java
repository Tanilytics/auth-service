package com.tanalytics.auth.security;

import com.tanalytics.auth.model.User;
import com.tanalytics.auth.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Loads a User from the database by either UUID (from JWT) or email (from login).
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user;
        try {
            // Try UUID first (used by JwtAuthFilter after token validation)
            UUID uuid = UUID.fromString(identifier);
            user = userRepository.findById(uuid)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));
        } catch (IllegalArgumentException e) {
            // Not a UUID — treat as email (used by DaoAuthenticationProvider during login)
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getId().toString())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase())))
                .build();
    }
}