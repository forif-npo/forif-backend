package org.forif_backend.application.study;

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
import org.forif_backend.domain.study.StudyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudyStartSchedulerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 12, 0);

    @Mock private SemesterService semesterService;
    @Mock private SemesterScheduleRepository semesterScheduleRepository;
    @Mock private StudyRepository studyRepository;
    @Mock private SemesterSchedule studyStartSchedule;

    @InjectMocks private StudyStartScheduler scheduler;

    @Test
    void 시작일이_도래하면_현재_학기의_승인_스터디를_시작_처리한다() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(semesterScheduleRepository.findByYearAndSemesterAndPhase(2026, 2, SemesterPhase.STUDY_START))
                .thenReturn(Optional.of(studyStartSchedule));
        when(studyStartSchedule.getStartsAt()).thenReturn(NOW.minusDays(1));

        scheduler.startStudies(NOW);

        verify(studyRepository).startPastApprovedStudies(2026, 2);
        verify(studyRepository).startApprovedStudies(2026, 2);
    }

    @Test
    void 시작일_전에는_현재_학기를_시작_처리하지_않는다() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(semesterScheduleRepository.findByYearAndSemesterAndPhase(2026, 2, SemesterPhase.STUDY_START))
                .thenReturn(Optional.of(studyStartSchedule));
        when(studyStartSchedule.getStartsAt()).thenReturn(NOW.plusDays(1));

        scheduler.startStudies(NOW);

        verify(studyRepository).startPastApprovedStudies(2026, 2);
        verify(studyRepository, never()).startApprovedStudies(2026, 2);
    }
}
