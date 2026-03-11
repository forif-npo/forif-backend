package org.forif_backend.application.semesterSchedule;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.semesterSchedule.dto.SemesterScheduleDto;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.common.SemesterSchedule;
import org.forif_backend.domain.common.SemesterScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SemesterScheduleService {

    private final SemesterScheduleRepository semesterScheduleRepository;

    @Transactional(readOnly = true)
    public List<SemesterScheduleDto> getAllSchedules() {
        return semesterScheduleRepository.findAll().stream()
                .map(SemesterScheduleDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SemesterScheduleDto> getSchedulesByYearAndSemester(int actYear, int actSemester) {
        return semesterScheduleRepository.findByYearAndSemester(actYear, actSemester).stream()
                .map(SemesterScheduleDto::from)
                .toList();
    }

    @Transactional
    public SemesterScheduleDto createSchedule(int actYear, int actSemester, String scheduleType, LocalDateTime scheduledAt) {
        SemesterSchedule schedule = SemesterSchedule.create(actYear, actSemester, scheduleType, scheduledAt);
        semesterScheduleRepository.save(schedule);
        return SemesterScheduleDto.from(schedule);
    }

    @Transactional
    public SemesterScheduleDto updateSchedule(Long id, String scheduleType, LocalDateTime scheduledAt) {
        SemesterSchedule schedule = semesterScheduleRepository.findById(id)
                .orElseThrow(() -> new ForifException(ErrorCode.SEMESTER_SCHEDULE_NOT_FOUND));
        schedule.update(scheduleType, scheduledAt);
        return SemesterScheduleDto.from(schedule);
    }

    @Transactional
    public void deleteSchedule(Long id) {
        semesterScheduleRepository.findById(id)
                .orElseThrow(() -> new ForifException(ErrorCode.SEMESTER_SCHEDULE_NOT_FOUND));
        semesterScheduleRepository.deleteById(id);
    }
}
