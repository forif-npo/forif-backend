package org.forif_backend.application.study;

import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.domain.study.RecruitStatus;
import org.forif_backend.domain.study.StudyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyRecruitStatusSchedulerTest {

    @Mock
    private SemesterService semesterService;

    @Mock
    private StudyRecruitStatusPolicy recruitStatusPolicy;

    @Mock
    private StudyRepository studyRepository;

    @InjectMocks
    private StudyRecruitStatusScheduler scheduler;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 12, 0);

    @Test
    void 모집_기간_안이면_활동_학기_스터디를_APPLICABLE로_동기화한다() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(recruitStatusPolicy.resolve(2026, 2, NOW)).thenReturn(RecruitStatus.APPLICABLE);

        scheduler.synchronizeRecruitStatuses(NOW);

        verify(studyRepository).updateRecruitStatusForApprovedStudies(2026, 2, RecruitStatus.APPLICABLE);
        verify(studyRepository).closeRecruitmentForNonActiveApprovedStudies(2026, 2);
    }

    @Test
    void 모집_기간_밖이면_CLOSED로_동기화한다() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(recruitStatusPolicy.resolve(2026, 2, NOW)).thenReturn(RecruitStatus.CLOSED);

        scheduler.synchronizeRecruitStatuses(NOW);

        verify(studyRepository).updateRecruitStatusForApprovedStudies(2026, 2, RecruitStatus.CLOSED);
        verify(studyRepository).closeRecruitmentForNonActiveApprovedStudies(2026, 2);
    }

    /**
     * 멘티 모집 일정이 없는 학기는 상시 모집이 아니라 모집 마감으로 동기화한다.
     */
    @Test
    void 일정이_없는_학기는_CLOSED로_동기화한다() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(recruitStatusPolicy.resolve(2026, 2, NOW)).thenReturn(RecruitStatus.CLOSED);

        scheduler.synchronizeRecruitStatuses(NOW);

        verify(studyRepository).updateRecruitStatusForApprovedStudies(2026, 2, RecruitStatus.CLOSED);
        verify(studyRepository).closeRecruitmentForNonActiveApprovedStudies(2026, 2);
    }
}
