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
 * 멘티 모집은 기간을 설정하지 않으면 닫는다. 그 외 단계는 설정을 잊었다고 동아리 운영이
 * 통째로 멈추지 않도록 기존 상시 개방 정책을 유지한다.
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

        // 멘티 모집은 일정을 명시적으로 설정해야만 연다. 그 외 단계는 기존 상시 개방 정책을 유지한다.
        if (schedule.isEmpty()) {
            if (phase == SemesterPhase.MENTEE_RECRUIT) {
                throw new ForifException(ErrorCode.SEMESTER_PHASE_NOT_STARTED);
            }
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

    /** 멘티 모집은 일정이 없으면 닫고, 그 외 단계는 기존 상시 개방 정책을 적용한다. */
    @Transactional(readOnly = true)
    public boolean isOpen(SemesterPhase phase, int actYear, int actSemester) {
        return semesterScheduleRepository
                .findByYearAndSemesterAndPhase(actYear, actSemester, phase)
                .map(schedule -> schedule.contains(LocalDateTime.now()))
                .orElse(phase != SemesterPhase.MENTEE_RECRUIT);
    }

    /**
     * 해당 단계의 시작 전까지만 허용한다. 일정이 없으면 기존 fail-open 정책을 따른다.
     */
    @Transactional(readOnly = true)
    public void requireBeforeStart(SemesterPhase phase) {
        SemesterInfo active = semesterService.getActive();
        requireBeforeStart(phase, active.actYear(), active.actSemester());
    }

    @Transactional(readOnly = true)
    public void requireBeforeStart(SemesterPhase phase, int actYear, int actSemester) {
        Optional<SemesterSchedule> schedule =
                semesterScheduleRepository.findByYearAndSemesterAndPhase(actYear, actSemester, phase);
        if (schedule.isEmpty() || schedule.get().notStartedAt(LocalDateTime.now())) {
            return;
        }

        SemesterSchedule window = schedule.get();
        throw new ForifException(ErrorCode.SEMESTER_PHASE_CLOSED, List.of(new ApiErrorData(
                phase.name(),
                "%s 시작 전까지만 가능합니다. 시작 시각은 %s입니다.".formatted(
                        phase.getLabel(),
                        window.getStartsAt().format(DISPLAY)),
                null
        )));
    }

    /** 일정이 없으면 상시 허용으로 간주해 수정 가능 여부 응답에도 동일하게 반영한다. */
    @Transactional(readOnly = true)
    public boolean isBeforeStart(SemesterPhase phase, int actYear, int actSemester) {
        return semesterScheduleRepository
                .findByYearAndSemesterAndPhase(actYear, actSemester, phase)
                .map(schedule -> schedule.notStartedAt(LocalDateTime.now()))
                .orElse(true);
    }

    /**
     * 모집 시작 전에는 허용하되, 해당 모집 단계가 끝난 뒤에는 새 대상을 만들지 못하게 한다.
     * 일정이 없는 학기는 기존 fail-open 정책을 따른다.
     */
    @Transactional(readOnly = true)
    public void requireNotEnded(SemesterPhase phase, int actYear, int actSemester) {
        Optional<SemesterSchedule> schedule =
                semesterScheduleRepository.findByYearAndSemesterAndPhase(actYear, actSemester, phase);
        if (schedule.isPresent() && !LocalDateTime.now().isBefore(schedule.get().getEndsAt())) {
            throw new ForifException(ErrorCode.SEMESTER_PHASE_CLOSED);
        }
    }
}
