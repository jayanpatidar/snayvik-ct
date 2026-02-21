package com.snayvik.kpi.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AccessGovernanceServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserSkillGroupRepository userSkillGroupRepository;

    @Mock
    private AccessAuditLogRepository accessAuditLogRepository;

    @Mock
    private ExternalAccessAdapter gitHubAccessAdapter;

    @Mock
    private ExternalAccessAdapter mondayAccessAdapter;

    private AccessGovernanceService accessGovernanceService;

    @BeforeEach
    void setUp() {
        accessGovernanceService = new AccessGovernanceService(
                userAccountRepository,
                userSkillGroupRepository,
                accessAuditLogRepository,
                List.of(gitHubAccessAdapter, mondayAccessAdapter));
    }

    @Test
    void blocksSelfDeactivation() {
        assertThatThrownBy(() -> accessGovernanceService.deactivateUser("u1", "u1"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void blocksLastAdminRemoval() {
        UserAccount account = new UserAccount();
        account.setId("u2");
        account.setEmail("admin@snayvik.com");
        account.setName("Admin");
        account.setActive(true);
        when(userAccountRepository.findById("u2")).thenReturn(Optional.of(account));
        when(userSkillGroupRepository.countAdminMembership("u2")).thenReturn(1L);
        when(userSkillGroupRepository.countActiveAdmins()).thenReturn(1L);

        assertThatThrownBy(() -> accessGovernanceService.deactivateUser("u2", "admin2"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void deactivatesUserAndWritesAuditLogs() throws Exception {
        when(gitHubAccessAdapter.systemName()).thenReturn("GITHUB");
        when(mondayAccessAdapter.systemName()).thenReturn("MONDAY");
        UserAccount account = new UserAccount();
        account.setId("u3");
        account.setEmail("dev@snayvik.com");
        account.setName("Dev");
        account.setActive(true);
        when(userAccountRepository.findById("u3")).thenReturn(Optional.of(account));
        when(userSkillGroupRepository.countAdminMembership("u3")).thenReturn(0L);
        when(userSkillGroupRepository.countActiveAdmins()).thenReturn(2L);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount result = accessGovernanceService.deactivateUser("u3", "admin1");

        assertThat(result.isActive()).isFalse();
        assertThat(result.getDeactivatedAt()).isNotNull();
        verify(gitHubAccessAdapter).revokeAllAccess(account);
        verify(mondayAccessAdapter).revokeAllAccess(account);
        verify(accessAuditLogRepository, times(2)).save(any(AccessAuditLog.class));
    }
}
