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
 * 멘티 모집 일정이 없거나 기간 밖이면 모집을 닫는다. 모집 기간을 명시적으로 설정해야만
 * 화면과 신청 API가 멘티 모집을 열 수 있다.
 */
@Component
@RequiredArgsConstructor
public class StudyRecruitStatusPolicy {

    private final SemesterScheduleRepository semesterScheduleRepository;

    public RecruitStatus resolve(int actYear, int actSemester, LocalDateTime now) {
        return semesterScheduleRepository
                .findByYearAndSemesterAndPhase(actYear, actSemester, SemesterPhase.MENTEE_RECRUIT)
                .map(schedule -> schedule.contains(now) ? RecruitStatus.APPLICABLE : RecruitStatus.CLOSED)
                .orElse(RecruitStatus.CLOSED);
    }
}
