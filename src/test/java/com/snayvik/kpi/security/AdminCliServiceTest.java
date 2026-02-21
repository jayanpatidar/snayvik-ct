package com.snayvik.kpi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminCliServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Captor
    private ArgumentCaptor<AuthUser> authUserCaptor;

    private AdminCliService adminCliService;

    @BeforeEach
    void setUp() {
        adminCliService = new AdminCliService(authUserRepository, passwordEncoder);
    }

    @Test
    void createsNewAdminWhenUserMissing() {
        when(authUserRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Secret@123")).thenReturn("encoded-password");

        adminCliService.createOrUpdateAdmin("admin", "Secret@123");

        org.mockito.Mockito.verify(authUserRepository).save(authUserCaptor.capture());
        AuthUser saved = authUserCaptor.getValue();
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(saved.getRole()).isEqualTo("ADMIN");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void updatesExistingAdmin() {
        AuthUser existing = new AuthUser();
        existing.setId(9L);
        existing.setUsername("admin");
        existing.setRole("USER");
        existing.setActive(false);

        when(authUserRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode(anyString())).thenReturn("new-encoded");

        adminCliService.createOrUpdateAdmin(" admin ", "Secret@123");

        org.mockito.Mockito.verify(authUserRepository).save(authUserCaptor.capture());
        AuthUser saved = authUserCaptor.getValue();
        assertThat(saved.getId()).isEqualTo(9L);
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.getPasswordHash()).isEqualTo("new-encoded");
        assertThat(saved.getRole()).isEqualTo("ADMIN");
        assertThat(saved.isActive()).isTrue();
    }
}
