package org.forif_backend.application.study.dto;

import lombok.Builder;

import java.util.List;

/**
 * 스터디 출석 현황 조회 결과 (멘티 × 주차)
 */
@Builder
public record StudyAttendanceResult(
        Integer studyId,
        String studyName,
        List<MenteeAttendance> mentees
) {
    @Builder
    public record MenteeAttendance(
            Long userId,
            String userName,
            String department,
            List<AttendanceRecord> records
    ) {
    }

    @Builder
    public record AttendanceRecord(
            int weekNum,
            String status,
            String studyDate
    ) {
    }
}
