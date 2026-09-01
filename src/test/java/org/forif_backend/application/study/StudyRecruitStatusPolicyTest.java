package org.forif_backend.application.study;

import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.semester.SemesterSchedule;
import org.forif_backend.domain.semester.SemesterScheduleRepository;
import org.forif_backend.domain.study.RecruitStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyRecruitStatusPolicyTest {

    @Mock
    private SemesterScheduleRepository semesterScheduleRepository;

    @InjectMocks
    private StudyRecruitStatusPolicy policy;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 12, 0);

    private void givenSchedule(LocalDateTime startsAt, LocalDateTime endsAt) {
        when(semesterScheduleRepository.findByYearAndSemesterAndPhase(2026, 2, SemesterPhase.MENTEE_RECRUIT))
                .thenReturn(Optional.of(SemesterSchedule.create(
                        2026, 2, SemesterPhase.MENTEE_RECRUIT, startsAt, endsAt, 1L)));
    }

    @Test
    void 모집_기간_안이면_APPLICABLE() {
        givenSchedule(NOW.minusDays(1), NOW.plusDays(1));

        assertThat(policy.resolve(2026, 2, NOW)).isEqualTo(RecruitStatus.APPLICABLE);
    }

    @Test
    void 모집_시작_전이면_CLOSED() {
        givenSchedule(NOW.plusDays(1), NOW.plusDays(8));

        assertThat(policy.resolve(2026, 2, NOW)).isEqualTo(RecruitStatus.CLOSED);
    }

    @Test
    void 모집_종료_후면_CLOSED() {
        givenSchedule(NOW.minusDays(8), NOW.minusDays(1));

        assertThat(policy.resolve(2026, 2, NOW)).isEqualTo(RecruitStatus.CLOSED);
    }

    @Test
    void 종료_시각_당일은_반열림_구간이라_CLOSED() {
        givenSchedule(NOW.minusDays(1), NOW);

        assertThat(policy.resolve(2026, 2, NOW)).isEqualTo(RecruitStatus.CLOSED);
    }

    /** 멘티 모집 일정을 설정하지 않은 학기에는 모집을 열지 않는다. */
    @Test
    void 일정이_없으면_CLOSED() {
        when(semesterScheduleRepository.findByYearAndSemesterAndPhase(2026, 2, SemesterPhase.MENTEE_RECRUIT))
                .thenReturn(Optional.empty());

        assertThat(policy.resolve(2026, 2, NOW)).isEqualTo(RecruitStatus.CLOSED);
    }
}
