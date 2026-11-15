package org.forif_backend.application.staffrecruitment.dto;

import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestionType;

/**
 * 자기소개서 문항 입력. 생성·수정 모두 전체 목록을 받아 교체한다.
 */
public record StaffRecruitmentQuestionCommand(
        String label,
        String description,
        StaffRecruitmentQuestionType type,
        boolean required,
        int maxLength,
        int displayOrder
) {
}
