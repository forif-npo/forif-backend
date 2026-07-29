package org.forif_backend.application.semester.dto;

/**
 * 학기 전환 전 영향 미리보기.
 * 전환 후 되돌리기 어려운 사고(빈 운영진 페이지, 잘못된 수료증 서명)를 미리 알린다.
 */
public record SemesterChangePreview(
        SemesterInfo current,
        SemesterInfo target,
        /** 대상 학기의 운영진 명단 수 (0이면 운영진 소개 페이지가 빈다) */
        int targetTeamMemberCount,
        /** 대상 학기에 등록된 해커톤 존재 여부 */
        boolean targetHackathonExists,
        /** 현재 학기 수강생 수 */
        long currentMemberCount,
        /** 현재 학기 수료증 발급 완료 수 */
        long currentCertificateIssuedCount
) {
    /** 전환 후 운영진을 새로 지정해야 하는지 */
    public boolean needsTeamSetup() {
        return targetTeamMemberCount == 0;
    }

    /**
     * 수료증이 아직 다 나가지 않았는지.
     * 전환 후 발급하면 신임 회장 서명이 찍히므로 전환 전 발급을 권한다.
     */
    public boolean hasPendingCertificates() {
        return currentMemberCount > currentCertificateIssuedCount;
    }
}
