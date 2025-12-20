package org.forif_backend.web.staff.dto;

import lombok.Builder;

@Builder
public record StaffInfoResponse(
        Long userId,
        String userName,
        String email,
        String phoneNum,
        String department,
        String imgUrl,
        String role
) {
}
