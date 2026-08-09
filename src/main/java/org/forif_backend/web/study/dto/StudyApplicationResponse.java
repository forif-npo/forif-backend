package org.forif_backend.web.study.dto;

import org.forif_backend.application.study.dto.StudyApplicationDto;

import java.time.LocalDateTime;
import java.util.List;

public record StudyApplicationResponse(
        Integer id,
        String studyName,
        String oneLiner,
        List<String> tags,
        String studyStatus,
        String rejectReason,
        LocalDateTime createdAt,
        boolean canModify
) {
    public static StudyApplicationResponse from(StudyApplicationDto dto) {
        return new StudyApplicationResponse(
                dto.getId(),
                dto.getStudyName(),
                dto.getOneLiner(),
                dto.getTags(),
                dto.getStudyStatus().getValue(),
                dto.getRejectReason(),
                dto.getCreatedAt(),
                dto.isCanModify()
        );
    }
}
