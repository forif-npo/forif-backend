package org.forif_backend.application.semester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.util.List;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.semester.SemesterScheduleRepository;
import org.junit.jupiter.api.Test;

class SemesterScheduleServiceTest {

    private final SemesterScheduleService service = new SemesterScheduleService(
            mock(SemesterScheduleRepository.class));

    @Test
    void rejectsScheduleTimesMorePreciseThanMinutes() {
        List<SemesterScheduleService.PhaseWindow> windows = List.of(
                new SemesterScheduleService.PhaseWindow(
                        SemesterPhase.MENTOR_RECRUIT,
                        LocalDateTime.of(2026, 3, 2, 9, 30, 1),
                        LocalDateTime.of(2026, 3, 2, 10, 0)
                ));

        assertThatThrownBy(() -> service.replaceSchedules(2026, 1, windows, 1L))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.SEMESTER_SCHEDULE_INVALID_RANGE));
    }
}
