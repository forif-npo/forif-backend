package org.forif_backend.application.semester;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.common.dto.response.ApiErrorData;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.semester.SemesterSchedule;
import org.forif_backend.domain.semester.SemesterScheduleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 모집 단계 기간 검사.
 *
 * 기간이 설정되지 않은 학기는 통과시킨다. 설정을 잊었다고 동아리 운영이
 * 통째로 멈추는 편이 더 위험하다고 보고 이렇게 정했다. 대신 어드민 화면에서
 * "이번 학기 일정 미설정"을 상시로 알린다.
 *
 * AOP가 아니라 서비스 계층에서 명시적으로 호출한다. 이 코드베이스에 애스펙트가
 * 하나도 없어 실행 경로를 소스에서 추적할 수 없게 되고, 자기호출 시 게이트가
 * 조용히 사라지는 실패 모드가 생기기 때문이다.
 */
@Component
@RequiredArgsConstructor
public class SemesterPhaseGuard {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm");

    private final SemesterScheduleRepository semesterScheduleRepository;
    private final SemesterService semesterService;

    /** 현재 활동 학기 기준으로 해당 단계가 열려 있는지 검사한다. 닫혀 있으면 예외를 던진다. */
    @Transactional(readOnly = true)
    public void requireOpen(SemesterPhase phase) {
        SemesterInfo active = semesterService.getActive();
        requireOpen(phase, active.actYear(), active.actSemester());
    }

    @Transactional(readOnly = true)
    public void requireOpen(SemesterPhase phase, int actYear, int actSemester) {
        Optional<SemesterSchedule> schedule =
                semesterScheduleRepository.findByYearAndSemesterAndPhase(actYear, actSemester, phase);

        // 기간 미설정 = 상시 개방
        if (schedule.isEmpty()) {
            return;
        }

        SemesterSchedule window = schedule.get();
        LocalDateTime now = LocalDateTime.now();
        if (window.contains(now)) {
            return;
        }

        ErrorCode errorCode = window.notStartedAt(now)
                ? ErrorCode.SEMESTER_PHASE_NOT_STARTED
                : ErrorCode.SEMESTER_PHASE_CLOSED;

        throw new ForifException(errorCode, List.of(new ApiErrorData(
                phase.name(),
                "%s 기간은 %s부터 %s까지입니다.".formatted(
                        phase.getLabel(),
                        window.getStartsAt().format(DISPLAY),
                        window.getEndsAt().format(DISPLAY)),
                null
        )));
    }
}
