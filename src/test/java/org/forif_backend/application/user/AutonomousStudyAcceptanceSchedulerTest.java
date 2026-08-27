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
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AutonomousStudyAcceptanceSchedulerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 12, 0);

    @Mock private SemesterService semesterService;
    @Mock private SemesterScheduleRepository semesterScheduleRepository;
    @Mock private StudyRepository studyRepository;
    @Mock private UserApplyService userApplyService;
    @Mock private SemesterSchedule menteeReviewSchedule;
    @Mock private Study autonomousStudy;

    @InjectMocks private AutonomousStudyAcceptanceScheduler scheduler;

    @Test
    void acceptsAutonomousStudyApplicationsOnlyDuringTheMenteeReviewPeriod() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(semesterScheduleRepository.findByYearAndSemesterAndPhase(2026, 2, SemesterPhase.MENTEE_REVIEW))
                .thenReturn(Optional.of(menteeReviewSchedule));
        when(menteeReviewSchedule.contains(NOW)).thenReturn(true);
        when(studyRepository.findAutonomousStudyByYearSemester(2026, 2))
                .thenReturn(Optional.of(autonomousStudy));
        when(userApplyService.acceptPendingAutonomousStudyApplications(autonomousStudy, 2026, 2))
                .thenReturn(3);

        scheduler.acceptAutonomousStudyApplications(NOW);

        verify(userApplyService).acceptPendingAutonomousStudyApplications(autonomousStudy, 2026, 2);
    }

    @Test
    void doesNotAcceptAutonomousStudyApplicationsOutsideTheMenteeReviewPeriod() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(semesterScheduleRepository.findByYearAndSemesterAndPhase(2026, 2, SemesterPhase.MENTEE_REVIEW))
                .thenReturn(Optional.of(menteeReviewSchedule));
        when(menteeReviewSchedule.contains(NOW)).thenReturn(false);

        scheduler.acceptAutonomousStudyApplications(NOW);

        verify(studyRepository, never()).findAutonomousStudyByYearSemester(2026, 2);
        verify(userApplyService, never()).acceptPendingAutonomousStudyApplications(autonomousStudy, 2026, 2);
    }
}
