package org.forif_backend.application.staffrecruitment.dto;

/**
 * 모집 부서 입력.
 * {@code id}가 있으면 그 부서를 수정하고, 없으면 새로 추가한다. 보낸 목록에서 빠진 부서는 지운다.
 */
public record StaffRecruitmentDepartmentCommand(
        Integer id,
        String name,
        String description,
        int displayOrder,
        boolean active
) {
}
