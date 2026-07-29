package org.forif_backend.application.semester.dto;

/**
 * 학기 전환 전 영향 미리보기.
 * 전환 후 운영진 소개 페이지가 비는 등의 사고를 막기 위해 사전에 확인시킨다.
 */
public record SemesterChangePreview(
        SemesterInfo current,
        SemesterInfo target,
        /** 대상 학기의 운영진 이력 수 (0이면 운영진 소개 페이지가 빈다) */
        int targetTeamMemberCount,
        /** 현재 학기의 운영진 이력 수 (복제 시 생성될 건수) */
        int currentTeamMemberCount,
        /** 대상 학기에 등록된 해커톤 존재 여부 */
        boolean targetHackathonExists
) {
    public boolean needsTeamCopy() {
        return targetTeamMemberCount == 0 && currentTeamMemberCount > 0;
    }
}
