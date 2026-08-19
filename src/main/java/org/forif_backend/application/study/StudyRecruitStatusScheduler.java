package org.forif_backend.application.study;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.domain.study.RecruitStatus;
import org.forif_backend.domain.study.StudyRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 현재 활동 학기의 멘티 모집 일정에 따라 승인된 스터디의 모집 상태를 자동으로 동기화한다.
 *
 * 일정이 없는 학기도 대상에 넣는다. 예전에는 건드리지 않고 넘겼는데, 그러면 모집 상태가
 * NULL로 남아 화면에서 "마감"으로 보였다. 정작 SemesterPhaseGuard는 일정이 없으면
 * 신청을 통과시키므로 실제로는 신청이 되는 상태였다. 판정은 StudyRecruitStatusPolicy에 맡긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudyRecruitStatusScheduler {

    private final SemesterService semesterService;
    private final StudyRecruitStatusPolicy recruitStatusPolicy;
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
        RecruitStatus targetStatus =
                recruitStatusPolicy.resolve(activeSemester.actYear(), activeSemester.actSemester(), now);

        int updatedCount = studyRepository.updateRecruitStatusForApprovedStudies(
                activeSemester.actYear(), activeSemester.actSemester(), targetStatus);
        updatedCount += studyRepository.closeRecruitmentForNonActiveApprovedStudies(
                activeSemester.actYear(), activeSemester.actSemester());

        if (updatedCount > 0) {
            log.info("스터디 모집 상태 동기화: {}년 {}학기 {} → {}건 변경",
                    activeSemester.actYear(), activeSemester.actSemester(), targetStatus, updatedCount);
        }
    }
}
