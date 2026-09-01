package org.forif_backend.web.semester.dto;

import org.forif_backend.application.semester.dto.SemesterInfo;

import java.util.List;

public record SemesterResponse(
        int actYear,
        int actSemester,
        String label
) {
    public static SemesterResponse from(SemesterInfo info) {
        return new SemesterResponse(info.actYear(), info.actSemester(), info.label());
    }

    public static List<SemesterResponse> fromList(List<SemesterInfo> infos) {
        return infos.stream().map(SemesterResponse::from).toList();
    }
}
