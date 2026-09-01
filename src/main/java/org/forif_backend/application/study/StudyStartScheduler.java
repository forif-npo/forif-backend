package org.forif_backend.application.study;

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

/** 학기 관리의 스터디 시작 시각에 맞춰 승인된 스터디를 실제 개설 상태로 전환한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudyStartScheduler {

    private static final ZoneId KOREA_STANDARD_TIME = ZoneId.of("Asia/Seoul");

    private final SemesterService semesterService;
    private final SemesterScheduleRepository semesterScheduleRepository;
    private final StudyRepository studyRepository;

    @Scheduled(
            initialDelayString = "${study.start-sync.initial-delay-ms:0}",
            fixedDelayString = "${study.start-sync.fixed-delay-ms:30000}"
    )
    @Transactional
    public void startStudies() {
        startStudies(LocalDateTime.now(KOREA_STANDARD_TIME));
    }

    void startStudies(LocalDateTime now) {
        SemesterInfo active = semesterService.getActive();
        int migratedCount = studyRepository.startPastApprovedStudies(active.actYear(), active.actSemester());
        int startedCount = semesterScheduleRepository
                .findByYearAndSemesterAndPhase(active.actYear(), active.actSemester(), SemesterPhase.STUDY_START)
                .filter(schedule -> !now.isBefore(schedule.getStartsAt()))
                .map(schedule -> studyRepository.startApprovedStudies(active.actYear(), active.actSemester()))
                .orElse(0);

        if (migratedCount > 0) {
            log.info("과거 학기 승인 스터디 시작 상태 전환: {}건", migratedCount);
        }
        if (startedCount > 0) {
            log.info("스터디 시작 시각 도래: {}년 {}학기 {}건 STARTED 전환",
                    active.actYear(), active.actSemester(), startedCount);
        }
    }
}
