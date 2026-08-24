package org.forif_backend.application.user.dto;

/**
 * 스터디 수강 신청.
 * 자율스터디는 priority와 applyReason이 모두 비어 있다.
 */
public record UserApplyCommand(
        Integer studyId,
        String applyReason,
        Integer priority
) {
}
