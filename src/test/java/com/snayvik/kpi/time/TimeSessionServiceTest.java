package com.snayvik.kpi.time;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TimeSessionServiceTest {

    @Mock
    private TimeSessionRepository timeSessionRepository;

    private TimeSessionService timeSessionService;

    @BeforeEach
    void setUp() {
        timeSessionService = new TimeSessionService(timeSessionRepository);
    }

    @Test
    void startsLiveSession() {
        when(timeSessionRepository.findFirstByUserIdAndEndTimeIsNullOrderByStartTimeDesc("dev1")).thenReturn(Optional.empty());
        when(timeSessionRepository.save(any(TimeSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TimeSession session = timeSessionService.start("AB-1", "dev1");

        assertThat(session.getTaskKey()).isEqualTo("AB-1");
        assertThat(session.getUserId()).isEqualTo("dev1");
        assertThat(session.getSource()).isEqualTo(TimeSessionService.SOURCE_LIVE);
        assertThat(session.getStartTime()).isNotNull();
    }

    @Test
    void stopFailsWhenNoOpenSession() {
        when(timeSessionRepository.findFirstByUserIdAndEndTimeIsNullOrderByStartTimeDesc("dev1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timeSessionService.stop("dev1", "AB-1"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void manualEntryRequiresReason() {
        assertThatThrownBy(() -> timeSessionService.addManual(
                        "AB-1",
                        "dev1",
                        Instant.parse("2026-02-20T10:00:00Z"),
                        Instant.parse("2026-02-20T11:00:00Z"),
                        ""))
                .isInstanceOf(ResponseStatusException.class);
    }
}
