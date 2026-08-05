package org.forif_backend.application.study;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.semester.SemesterSchedule;
import org.forif_backend.domain.semester.SemesterScheduleRepository;
import org.forif_backend.domain.study.RecruitStatus;
import org.forif_backend.domain.study.StudyRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 현재 활동 학기의 멘티 모집 일정에 따라 승인된 스터디의 모집 상태를 자동으로 동기화한다.
 *
 * 멘티 모집 기간이 설정된 학기만 대상이며, 기간 안에서는 APPLICABLE,
 * 기간 전·후에는 CLOSED로 설정한다. 일정이 없는 학기는 기존 fail-open
 * 정책을 보존하기 위해 변경하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudyRecruitStatusScheduler {

    private final SemesterService semesterService;
    private final SemesterScheduleRepository semesterScheduleRepository;
    private final StudyRepository studyRepository;

    @Scheduled(
            initialDelayString = "${study.recruit-status-sync.initial-delay-ms:0}",
            fixedDelayString = "${study.recruit-status-sync.fixed-delay-ms:30000}"
    )
    @Transactional
    public void synchronizeRecruitStatuses() {
        synchronizeRecruitStatuses(LocalDateTime.now());
    }

    void synchronizeRecruitStatuses(LocalDateTime now) {
        SemesterInfo activeSemester = semesterService.getActive();
        semesterScheduleRepository.findByYearAndSemesterAndPhase(
                        activeSemester.actYear(), activeSemester.actSemester(), SemesterPhase.MENTEE_RECRUIT)
                .ifPresent(schedule -> synchronize(schedule, now));
    }

    private void synchronize(SemesterSchedule schedule, LocalDateTime now) {
        RecruitStatus targetStatus = schedule.contains(now)
                ? RecruitStatus.APPLICABLE
                : RecruitStatus.CLOSED;

        int updatedCount = studyRepository.updateRecruitStatusForApprovedStudies(
                schedule.getActYear(), schedule.getActSemester(), targetStatus);

        if (updatedCount > 0) {
            log.info("스터디 모집 상태 동기화: {}년 {}학기 {} → {}건 변경",
                    schedule.getActYear(), schedule.getActSemester(), targetStatus, updatedCount);
        }
    }
}
