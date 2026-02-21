package com.snayvik.kpi.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCliService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminCliService(AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void createOrUpdateAdmin(String username, String rawPassword) {
        String normalizedUsername = normalizeRequired(username, "username");
        String normalizedPassword = normalizeRequired(rawPassword, "password");

        AuthUser authUser = authUserRepository
                .findByUsernameIgnoreCase(normalizedUsername)
                .orElseGet(AuthUser::new);
        authUser.setUsername(normalizedUsername);
        authUser.setPasswordHash(passwordEncoder.encode(normalizedPassword));
        authUser.setRole("ADMIN");
        authUser.setActive(true);
        authUserRepository.save(authUser);
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }
}
