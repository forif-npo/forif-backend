package org.forif_backend.application.semester;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.semester.SemesterSchedule;
import org.forif_backend.domain.semester.SemesterScheduleRepository;
import org.junit.jupiter.api.Test;

class SemesterPhaseGuardTest {

    private final SemesterScheduleRepository scheduleRepository = mock(SemesterScheduleRepository.class);
    private final SemesterPhaseGuard guard = new SemesterPhaseGuard(
            scheduleRepository, mock(SemesterService.class));

    @Test
    void allowsCreatingAStudyBeforeMenteeRecruitmentStarts() {
        when(scheduleRepository.findByYearAndSemesterAndPhase(2026, 2, SemesterPhase.MENTEE_RECRUIT))
                .thenReturn(Optional.of(SemesterSchedule.create(
                        2026,
                        2,
                        SemesterPhase.MENTEE_RECRUIT,
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(7),
                        1L
                )));

        assertThatCode(() -> guard.requireNotEnded(SemesterPhase.MENTEE_RECRUIT, 2026, 2))
                .doesNotThrowAnyException();
    }

    @Test
    void blocksCreatingAStudyAfterMenteeRecruitmentEnds() {
        when(scheduleRepository.findByYearAndSemesterAndPhase(2026, 2, SemesterPhase.MENTEE_RECRUIT))
                .thenReturn(Optional.of(SemesterSchedule.create(
                        2026,
                        2,
                        SemesterPhase.MENTEE_RECRUIT,
                        LocalDateTime.now().minusDays(7),
                        LocalDateTime.now().minusDays(1),
                        1L
                )));

        assertThatThrownBy(() -> guard.requireNotEnded(SemesterPhase.MENTEE_RECRUIT, 2026, 2))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> org.assertj.core.api.Assertions.assertThat(
                        ((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.SEMESTER_PHASE_CLOSED));
    }

    @Test
    void allowsUpdatingAnApplicationBeforeMenteeRecruitmentStarts() {
        when(scheduleRepository.findByYearAndSemesterAndPhase(2026, 2, SemesterPhase.MENTEE_RECRUIT))
                .thenReturn(Optional.of(SemesterSchedule.create(
                        2026,
                        2,
                        SemesterPhase.MENTEE_RECRUIT,
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(7),
                        1L
                )));

        assertThatCode(() -> guard.requireBeforeStart(SemesterPhase.MENTEE_RECRUIT, 2026, 2))
                .doesNotThrowAnyException();
    }

    @Test
    void blocksUpdatingAnApplicationWhenMenteeRecruitmentHasStarted() {
        when(scheduleRepository.findByYearAndSemesterAndPhase(2026, 2, SemesterPhase.MENTEE_RECRUIT))
                .thenReturn(Optional.of(SemesterSchedule.create(
                        2026,
                        2,
                        SemesterPhase.MENTEE_RECRUIT,
                        LocalDateTime.now().minusSeconds(1),
                        LocalDateTime.now().plusDays(7),
                        1L
                )));

        assertThatThrownBy(() -> guard.requireBeforeStart(SemesterPhase.MENTEE_RECRUIT, 2026, 2))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> org.assertj.core.api.Assertions.assertThat(
                        ((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.SEMESTER_PHASE_CLOSED));
    }
}
