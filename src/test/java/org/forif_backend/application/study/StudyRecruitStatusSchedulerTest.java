package org.forif_backend.application.study;

import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.semester.SemesterSchedule;
import org.forif_backend.domain.semester.SemesterScheduleRepository;
import org.forif_backend.domain.study.RecruitStatus;
import org.forif_backend.domain.study.StudyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyRecruitStatusSchedulerTest {

    @Mock
    private SemesterScheduleRepository semesterScheduleRepository;

    @Mock
    private SemesterService semesterService;

    @Mock
    private StudyRepository studyRepository;

    @InjectMocks
    private StudyRecruitStatusScheduler scheduler;

    @Test
    void opensApprovedStudiesDuringMenteeRecruitment() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 12, 0);
        SemesterSchedule schedule = SemesterSchedule.create(
                2026, 2, SemesterPhase.MENTEE_RECRUIT,
                now.minusDays(1), now.plusDays(1), 1L);
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(semesterScheduleRepository.findByYearAndSemesterAndPhase(
                2026, 2, SemesterPhase.MENTEE_RECRUIT)).thenReturn(Optional.of(schedule));

        scheduler.synchronizeRecruitStatuses(now);

        verify(studyRepository).updateRecruitStatusForApprovedStudies(
                2026, 2, RecruitStatus.APPLICABLE);
    }

    @Test
    void closesApprovedStudiesOutsideMenteeRecruitment() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 5, 12, 0);
        SemesterSchedule schedule = SemesterSchedule.create(
                2026, 2, SemesterPhase.MENTEE_RECRUIT,
                now.plusDays(1), now.plusDays(8), 1L);
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(semesterScheduleRepository.findByYearAndSemesterAndPhase(
                2026, 2, SemesterPhase.MENTEE_RECRUIT)).thenReturn(Optional.of(schedule));
        when(studyRepository.updateRecruitStatusForApprovedStudies(
                any(Integer.class), any(Integer.class), any(RecruitStatus.class)))
                .thenReturn(0);

        scheduler.synchronizeRecruitStatuses(now);

        verify(studyRepository).updateRecruitStatusForApprovedStudies(
                2026, 2, RecruitStatus.CLOSED);
    }
}
