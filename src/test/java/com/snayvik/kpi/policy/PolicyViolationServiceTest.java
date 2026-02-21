package com.snayvik.kpi.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyViolationServiceTest {

    @Mock
    private PolicyViolationRepository policyViolationRepository;

    private PolicyViolationService policyViolationService;

    @BeforeEach
    void setUp() {
        policyViolationService = new PolicyViolationService(policyViolationRepository);
    }

    @Test
    void skipsDuplicateUnresolvedTaskViolation() {
        when(policyViolationRepository.existsByTaskKeyAndTypeAndResolvedFalse("AB-1", ViolationType.DONE_WITHOUT_MERGE))
                .thenReturn(true);

        policyViolationService.createViolation("AB-1", null, ViolationType.DONE_WITHOUT_MERGE, "HIGH", "message");

        verify(policyViolationRepository).existsByTaskKeyAndTypeAndResolvedFalse("AB-1", ViolationType.DONE_WITHOUT_MERGE);
    }

    @Test
    void resolvesViolationWithReason() {
        PolicyViolation violation = new PolicyViolation();
        when(policyViolationRepository.findById(10L)).thenReturn(Optional.of(violation));
        when(policyViolationRepository.save(violation)).thenReturn(violation);

        PolicyViolation resolved = policyViolationService.resolveViolation(10L, "admin@snayvik.com", "false positive");

        assertThat(resolved.isResolved()).isTrue();
        assertThat(resolved.getResolvedBy()).isEqualTo("admin@snayvik.com");
        assertThat(resolved.getResolvedReason()).isEqualTo("false positive");
        assertThat(resolved.getResolvedAt()).isNotNull();
    }

    @Test
    void createsViolationWhenNoDuplicateExists() {
        when(policyViolationRepository.existsByTaskKeyAndTypeAndResolvedFalse("AB-1", ViolationType.DONE_WITHOUT_MERGE))
                .thenReturn(false);

        policyViolationService.createViolation("AB-1", "dev1", ViolationType.DONE_WITHOUT_MERGE, "HIGH", "missing merge");

        ArgumentCaptor<PolicyViolation> captor = ArgumentCaptor.forClass(PolicyViolation.class);
        verify(policyViolationRepository).save(captor.capture());
        assertThat(captor.getValue().getTaskKey()).isEqualTo("AB-1");
        assertThat(captor.getValue().getUserId()).isEqualTo("dev1");
        assertThat(captor.getValue().getType()).isEqualTo(ViolationType.DONE_WITHOUT_MERGE);
    }
}
