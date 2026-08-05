package org.forif_backend.application.study;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.semester.SemesterScheduleRepository;
import org.forif_backend.domain.study.RecruitStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 승인된 스터디가 지금 가져야 할 모집 상태 판정.
 *
 * 스케줄러와 스터디 승인이 같은 규칙을 각자 구현하면 어긋나기 때문에 여기로 모았다.
 *
 * 멘티 모집 일정이 없는 학기는 APPLICABLE 이다. 신청을 실제로 막는 SemesterPhaseGuard 가
 * 일정 행이 없으면 통과시키므로(fail-open), 화면만 마감이라고 표시하면 거짓말이 된다.
 */
@Component
@RequiredArgsConstructor
public class StudyRecruitStatusPolicy {

    private final SemesterScheduleRepository semesterScheduleRepository;

    public RecruitStatus resolve(int actYear, int actSemester, LocalDateTime now) {
        return semesterScheduleRepository
                .findByYearAndSemesterAndPhase(actYear, actSemester, SemesterPhase.MENTEE_RECRUIT)
                .map(schedule -> schedule.contains(now) ? RecruitStatus.APPLICABLE : RecruitStatus.CLOSED)
                .orElse(RecruitStatus.APPLICABLE);
    }
}
