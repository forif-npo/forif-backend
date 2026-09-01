package org.forif_backend.application.study.dto;

/**
 * 출석 기록 upsert 명령
 */
public record AttendanceCommand(
        Long userId,
        int weekNum,
        String status,
        String studyDate
) {
}
