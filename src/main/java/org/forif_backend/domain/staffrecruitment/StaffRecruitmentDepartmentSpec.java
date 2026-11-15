package org.forif_backend.domain.staffrecruitment;

/**
 * 부서 병합 입력. {@code id}가 있으면 그 부서를 수정하고, 없으면 새로 추가한다.
 * 제출된 지원서가 부서를 참조하므로 전체 삭제·재삽입 대신 식별자로 맞춰 간다.
 */
public record StaffRecruitmentDepartmentSpec(
        Integer id,
        String name,
        String description,
        int displayOrder,
        boolean active
) {
}
