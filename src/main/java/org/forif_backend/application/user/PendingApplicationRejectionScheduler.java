package org.forif_backend.application.user;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.semester.SemesterScheduleRepository;
import org.forif_backend.domain.user.UserApplyRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 멘티 합불 처리 기간 종료 뒤 남은 PENDING 신청 건을 REJECT로 확정한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingApplicationRejectionScheduler {

    private static final ZoneId KOREA_STANDARD_TIME = ZoneId.of("Asia/Seoul");

    private final SemesterService semesterService;
    private final SemesterScheduleRepository semesterScheduleRepository;
    private final UserApplyRepository userApplyRepository;

    @Scheduled(
            initialDelayString = "${user-apply.pending-rejection-sync.initial-delay-ms:0}",
            fixedDelayString = "${user-apply.pending-rejection-sync.fixed-delay-ms:60000}"
    )
    @Transactional
    public void rejectPendingApplicationsAfterReviewEnd() {
        rejectPendingApplicationsAfterReviewEnd(LocalDateTime.now(KOREA_STANDARD_TIME));
    }

    void rejectPendingApplicationsAfterReviewEnd(LocalDateTime now) {
        SemesterInfo activeSemester = semesterService.getActive();
        semesterScheduleRepository
                .findByYearAndSemesterAndPhaseForUpdate(
                        activeSemester.actYear(), activeSemester.actSemester(), SemesterPhase.MENTEE_REVIEW)
                // MENTEE_REVIEW은 [startsAt, endsAt)이므로 종료 시각부터 자동 확정한다.
                .filter(schedule -> !now.isBefore(schedule.getEndsAt()))
                .ifPresent(schedule -> rejectPendingApplications(activeSemester));
    }

    private void rejectPendingApplications(SemesterInfo activeSemester) {
        int rejectedCount = userApplyRepository.rejectPendingApplicationsByYearSemester(
                activeSemester.actYear(), activeSemester.actSemester());
        if (rejectedCount > 0) {
            log.info("멘티 합불 처리 기간 종료: {}년 {}학기 신청서 {}건의 PENDING 상태를 REJECT로 확정",
                    activeSemester.actYear(), activeSemester.actSemester(), rejectedCount);
        }
    }
}
