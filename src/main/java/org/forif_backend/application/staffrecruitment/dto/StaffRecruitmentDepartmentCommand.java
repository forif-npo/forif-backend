package org.forif_backend.application.staffrecruitment.dto;

/**
 * 모집 부서 입력. 생성·수정 모두 전체 목록을 받아 교체한다.
 */
public record StaffRecruitmentDepartmentCommand(
        String name,
        String description,
        int displayOrder,
        boolean active
) {
}
