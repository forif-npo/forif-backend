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
    }

    @Test
    void 모집_기간_밖이면_CLOSED로_동기화한다() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(recruitStatusPolicy.resolve(2026, 2, NOW)).thenReturn(RecruitStatus.CLOSED);

        scheduler.synchronizeRecruitStatuses(NOW);

        verify(studyRepository).updateRecruitStatusForApprovedStudies(2026, 2, RecruitStatus.CLOSED);
    }

    /**
     * 예전에는 일정이 없는 학기를 건너뛰어 모집 상태가 NULL로 남았고,
     * 프론트가 NULL을 "마감"으로 그려서 실제 신청 가능 여부와 어긋났다.
     * 이제는 정책이 돌려주는 값으로 반드시 동기화한다.
     */
    @Test
    void 일정이_없는_학기도_건너뛰지_않는다() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(recruitStatusPolicy.resolve(2026, 2, NOW)).thenReturn(RecruitStatus.APPLICABLE);

        scheduler.synchronizeRecruitStatuses(NOW);

        verify(studyRepository).updateRecruitStatusForApprovedStudies(2026, 2, RecruitStatus.APPLICABLE);
    }
}
