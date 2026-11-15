package org.forif_backend.domain.staffrecruitment;

/**
 * 문항 병합 입력. {@code id}가 있으면 그 문항을 수정하고, 없으면 새로 추가한다.
 */
public record StaffRecruitmentQuestionSpec(
        Integer id,
        String label,
        String description,
        StaffRecruitmentQuestionType type,
        boolean required,
        int maxLength,
        int displayOrder
) {
}
