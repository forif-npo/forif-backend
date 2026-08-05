package org.forif_backend.application.semester;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.semester.dto.SemesterScheduleInfo;
import org.forif_backend.common.dto.response.ApiErrorData;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.semester.SemesterSchedule;
import org.forif_backend.domain.semester.SemesterScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 학기별 모집 단계 기간 관리 (회장단 전용).
 *
 * 단계는 순차적이어야 하며 겹칠 수 없다. 모집 창구가 열려 있는 동안 심사가
 * 진행되면 나중에 지원한 사람이 불리해지고, 멘토 심사가 안 끝났는데 멘티 모집이
 * 열리면 지원 기간 중에 스터디 목록이 계속 늘어나기 때문이다.
 */
@Service
@RequiredArgsConstructor
public class SemesterScheduleService {

    private final SemesterScheduleRepository semesterScheduleRepository;

    @Transactional(readOnly = true)
    public List<SemesterScheduleInfo> getSchedules(int actYear, int actSemester) {
        LocalDateTime now = LocalDateTime.now();
        return semesterScheduleRepository.findByYearAndSemester(actYear, actSemester).stream()
                .sorted(Comparator.comparing(SemesterSchedule::getPhase))
                .map(schedule -> SemesterScheduleInfo.from(schedule, now))
                .toList();
    }

    /**
     * 한 학기의 단계 기간을 통째로 저장한다.
     * 요청에 없는 단계는 삭제되므로, 부분 수정이 아니라 전체 교체다.
     */
    @Transactional
    public List<SemesterScheduleInfo> replaceSchedules(int actYear, int actSemester,
                                                       List<PhaseWindow> windows, Long updatedBy) {
        Map<SemesterPhase, PhaseWindow> requested = toValidatedMap(windows);
        validateOrder(requested);

        List<SemesterSchedule> existing = semesterScheduleRepository.findByYearAndSemester(actYear, actSemester);
        Map<SemesterPhase, SemesterSchedule> existingByPhase = new EnumMap<>(SemesterPhase.class);
        existing.forEach(schedule -> existingByPhase.put(schedule.getPhase(), schedule));

        // 요청에서 빠진 단계는 제거한다 (해당 단계를 상시 개방으로 되돌리는 수단)
        existingByPhase.forEach((phase, schedule) -> {
            if (!requested.containsKey(phase)) {
                semesterScheduleRepository.delete(schedule);
            }
        });

        List<SemesterSchedule> saved = new ArrayList<>();
        requested.forEach((phase, window) -> {
            SemesterSchedule schedule = existingByPhase.get(phase);
            if (schedule == null) {
                schedule = SemesterSchedule.create(
                        actYear, actSemester, phase, window.startsAt(), window.endsAt(), updatedBy);
            } else {
                schedule.update(window.startsAt(), window.endsAt(), updatedBy);
            }
            saved.add(semesterScheduleRepository.save(schedule));
        });

        LocalDateTime now = LocalDateTime.now();
        return saved.stream()
                .sorted(Comparator.comparing(SemesterSchedule::getPhase))
                .map(schedule -> SemesterScheduleInfo.from(schedule, now))
                .toList();
    }

    private Map<SemesterPhase, PhaseWindow> toValidatedMap(List<PhaseWindow> windows) {
        Map<SemesterPhase, PhaseWindow> map = new EnumMap<>(SemesterPhase.class);

        for (PhaseWindow window : windows) {
            if (!window.endsAt().isAfter(window.startsAt())) {
                throw new ForifException(ErrorCode.SEMESTER_SCHEDULE_INVALID_RANGE, List.of(new ApiErrorData(
                        window.phase().name(),
                        "%s 기간의 종료 시각이 시작 시각보다 앞서거나 같습니다.".formatted(window.phase().getLabel()),
                        null)));
            }
            if (map.putIfAbsent(window.phase(), window) != null) {
                throw new ForifException(ErrorCode.SEMESTER_SCHEDULE_INVALID_RANGE, List.of(new ApiErrorData(
                        window.phase().name(),
                        "%s 기간이 중복으로 들어왔습니다.".formatted(window.phase().getLabel()),
                        null)));
            }
        }
        return map;
    }

    /**
     * 설정된 단계끼리 순서와 겹침을 검사한다.
     * 미설정 단계는 건너뛰고 그다음 설정된 단계와 비교한다.
     */
    private void validateOrder(Map<SemesterPhase, PhaseWindow> requested) {
        PhaseWindow previous = null;

        for (SemesterPhase phase : SemesterPhase.inOrder()) {
            PhaseWindow current = requested.get(phase);
            if (current == null) {
                continue;
            }
            if (previous != null && current.startsAt().isBefore(previous.endsAt())) {
                throw new ForifException(ErrorCode.SEMESTER_SCHEDULE_ORDER_INVALID, List.of(new ApiErrorData(
                        phase.name(),
                        "%s 기간이 %s 기간이 끝나기 전에 시작합니다.".formatted(
                                phase.getLabel(), previous.phase().getLabel()),
                        null)));
            }
            previous = current;
        }
    }

    public record PhaseWindow(SemesterPhase phase, LocalDateTime startsAt, LocalDateTime endsAt) {
    }
}
