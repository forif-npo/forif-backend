package org.forif_backend.application.user;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.semester.SemesterScheduleRepository;
import org.forif_backend.domain.study.StudyRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 멘티 수락/거절 기간에 자율스터디 신청을 자동 합격 처리한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutonomousStudyAcceptanceScheduler {

    private static final ZoneId KOREA_STANDARD_TIME = ZoneId.of("Asia/Seoul");

    private final SemesterService semesterService;
    private final SemesterScheduleRepository semesterScheduleRepository;
    private final StudyRepository studyRepository;
    private final UserApplyService userApplyService;

    @Scheduled(
            initialDelayString = "${study.autonomous-acceptance.initial-delay-ms:0}",
            fixedDelayString = "${study.autonomous-acceptance.fixed-delay-ms:30000}"
    )
    @Transactional
    public void acceptAutonomousStudyApplications() {
        acceptAutonomousStudyApplications(LocalDateTime.now(KOREA_STANDARD_TIME));
    }

    void acceptAutonomousStudyApplications(LocalDateTime now) {
        SemesterInfo active = semesterService.getActive();
        boolean isMenteeReviewOpen = semesterScheduleRepository
                .findByYearAndSemesterAndPhase(active.actYear(), active.actSemester(), SemesterPhase.MENTEE_REVIEW)
                .map(schedule -> schedule.contains(now))
                .orElse(false);
        if (!isMenteeReviewOpen) {
            return;
        }

        studyRepository.findAutonomousStudyByYearSemester(active.actYear(), active.actSemester())
                .ifPresent(study -> {
                    int acceptedCount = userApplyService.acceptPendingAutonomousStudyApplications(
                            study, active.actYear(), active.actSemester());
                    if (acceptedCount > 0) {
                        log.info("자율스터디 자동 합격 처리: {}년 {}학기 {}명",
                                active.actYear(), active.actSemester(), acceptedCount);
                    }
                });
    }
}
