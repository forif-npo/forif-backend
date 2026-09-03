package org.forif_backend.application.user;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.semester.SemesterSchedule;
import org.forif_backend.domain.semester.SemesterScheduleRepository;
import org.forif_backend.domain.user.UserApplyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendingApplicationRejectionSchedulerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 4, 12, 0);

    @Mock private SemesterService semesterService;
    @Mock private SemesterScheduleRepository semesterScheduleRepository;
    @Mock private UserApplyRepository userApplyRepository;
    @Mock private SemesterSchedule menteeReviewSchedule;

    @InjectMocks private PendingApplicationRejectionScheduler scheduler;

    @Test
    void 합불처리기간이_끝나면_현재학기의_남은_대기신청을_불합격처리한다() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(semesterScheduleRepository.findByYearAndSemesterAndPhaseForUpdate(2026, 2, SemesterPhase.MENTEE_REVIEW))
                .thenReturn(Optional.of(menteeReviewSchedule));
        when(menteeReviewSchedule.getEndsAt()).thenReturn(NOW);

        scheduler.rejectPendingApplicationsAfterReviewEnd(NOW);

        verify(userApplyRepository).rejectPendingApplicationsByYearSemester(2026, 2);
    }

    @Test
    void 합불처리기간_종료전에는_대기신청을_변경하지_않는다() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(semesterScheduleRepository.findByYearAndSemesterAndPhaseForUpdate(2026, 2, SemesterPhase.MENTEE_REVIEW))
                .thenReturn(Optional.of(menteeReviewSchedule));
        when(menteeReviewSchedule.getEndsAt()).thenReturn(NOW.plusMinutes(1));

        scheduler.rejectPendingApplicationsAfterReviewEnd(NOW);

        verify(userApplyRepository, never()).rejectPendingApplicationsByYearSemester(2026, 2);
    }

    @Test
    void 합불처리일정이_없으면_자동불합격처리하지_않는다() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(semesterScheduleRepository.findByYearAndSemesterAndPhaseForUpdate(2026, 2, SemesterPhase.MENTEE_REVIEW))
                .thenReturn(Optional.empty());

        scheduler.rejectPendingApplicationsAfterReviewEnd(NOW);

        verify(userApplyRepository, never()).rejectPendingApplicationsByYearSemester(2026, 2);
    }
}
