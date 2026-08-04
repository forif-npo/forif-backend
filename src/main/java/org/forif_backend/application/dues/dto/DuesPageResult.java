package org.forif_backend.application.dues.dto;

import org.forif_backend.application.semester.dto.SemesterInfo;

import java.util.List;

public record DuesPageResult(
        SemesterInfo semester,
        DuesSummary summary,
        List<DuesMember> content,
        int totalElements,
        int currentPage,
        int totalPages,
        int pageSize
) {
}
