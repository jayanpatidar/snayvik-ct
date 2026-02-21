package com.snayvik.kpi.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {

    Optional<AuthUser> findByUsernameIgnoreCase(String username);

    Optional<AuthUser> findByUsernameIgnoreCaseAndActiveTrue(String username);
}
