package org.forif_backend.application.staffrecruitment;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** 종료 시각이 지난 게시 회차를 자동으로 마감한다. */
@Component
@RequiredArgsConstructor
public class StaffRecruitmentAutoCloseScheduler {

    private static final ZoneId KOREA_STANDARD_TIME = ZoneId.of("Asia/Seoul");
    private final StaffRecruitmentRepository staffRecruitmentRepository;

    @Scheduled(
            initialDelayString = "${staff-recruitment.auto-close.initial-delay-ms:0}",
            fixedDelayString = "${staff-recruitment.auto-close.fixed-delay-ms:30000}"
    )
    @Transactional
    public void closeExpiredRecruitment() {
        closeExpiredRecruitment(LocalDateTime.now(KOREA_STANDARD_TIME));
    }

    void closeExpiredRecruitment(LocalDateTime now) {
        staffRecruitmentRepository.findExpiredPublishedForUpdate(now)
                .ifPresent(recruitment -> recruitment.close("모집 기간 종료", now));
    }
}
