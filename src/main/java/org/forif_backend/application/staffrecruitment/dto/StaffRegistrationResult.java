package org.forif_backend.application.staffrecruitment.dto;

import java.util.List;

/**
 * 합격자 운영진 등록 결과. 이름은 지원서에 적힌 이름이다.
 *
 * @param registered     이번에 명단에 올린 합격자
 * @param alreadyMembers 이미 그 학기 명단에 있어 건너뛴 합격자
 * @param withdrawn      탈퇴해 회원 정보가 없어 올리지 못한 합격자
 */
public record StaffRegistrationResult(
        int actYear,
        int actSemester,
        List<String> registered,
        List<String> alreadyMembers,
        List<String> withdrawn
) {
}
