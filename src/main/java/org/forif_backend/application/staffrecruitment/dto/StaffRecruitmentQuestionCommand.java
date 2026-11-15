package org.forif_backend.application.staffrecruitment.dto;

import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestionType;

/**
 * 자기소개서 문항 입력.
 * {@code id}가 있으면 그 문항을 수정하고, 없으면 새로 추가한다. 보낸 목록에서 빠진 문항은 지운다.
 */
public record StaffRecruitmentQuestionCommand(
        Integer id,
        String label,
        String description,
        StaffRecruitmentQuestionType type,
        boolean required,
        int maxLength,
        int displayOrder
) {
}
