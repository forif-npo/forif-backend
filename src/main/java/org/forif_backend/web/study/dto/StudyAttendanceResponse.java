package org.forif_backend.web.study.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.forif_backend.application.study.dto.StudyAttendanceResult;

import java.util.List;

@Schema(description = "스터디 출석 현황 응답")
@Builder
public record StudyAttendanceResponse(
        @Schema(description = "스터디 ID", example = "102")
        Integer studyId,

        @Schema(description = "스터디 이름", example = "README.md")
        String studyName,

        @Schema(description = "멘티별 출석 기록")
        List<MenteeAttendance> mentees
) {
    @Builder
    public record MenteeAttendance(
            @Schema(description = "멘티 유저 ID(학번)", example = "2024097956")
            Long userId,

            @Schema(description = "멘티 이름", example = "홍길동")
            String userName,

            @Schema(description = "학과", example = "컴퓨터소프트웨어학부")
            String department,

            @Schema(description = "주차별 출석 기록")
            List<AttendanceRecord> records
    ) {
    }

    @Builder
    public record AttendanceRecord(
            @Schema(description = "주차", example = "1")
            int weekNum,

            @Schema(description = "출석 상태 (present / absent)", example = "present")
            String status,

            @Schema(description = "스터디 진행일 (yyyy-MM-dd)", example = "2026-03-06")
            String studyDate
    ) {
    }

    public static StudyAttendanceResponse from(StudyAttendanceResult result) {
        return StudyAttendanceResponse.builder()
                .studyId(result.studyId())
                .studyName(result.studyName())
                .mentees(result.mentees().stream()
                        .map(m -> MenteeAttendance.builder()
                                .userId(m.userId())
                                .userName(m.userName())
                                .department(m.department())
                                .records(m.records().stream()
                                        .map(r -> AttendanceRecord.builder()
                                                .weekNum(r.weekNum())
                                                .status(r.status())
                                                .studyDate(r.studyDate())
                                                .build())
                                        .toList())
                                .build())
                        .toList())
                .build();
    }
}
