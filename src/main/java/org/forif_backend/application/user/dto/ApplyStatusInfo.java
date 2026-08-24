package org.forif_backend.application.user.dto;

import lombok.Builder;
import org.forif_backend.application.study.dto.StudyDto;

/** 현재 학기의 지원 가능 여부와 이미 지원한 스터디. */
@Builder
public record ApplyStatusInfo(
        boolean canApplyPrimary,
        boolean canApplySecondary,
        boolean canApplyAutonomousStudy,
        boolean hasAutonomousStudyApplication,
        StudyDto primaryStudy,
        StudyDto secondaryStudy
) {
}
