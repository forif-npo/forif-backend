package org.forif_backend.web.staff.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record StaffSignInRequest(
        Long userId,
        String password,

        @Schema(description = "로그인할 계정 역할 (MENTOR / ADMIN). 미지정 시 비밀번호가 일치하는 계정으로 로그인 (ADMIN 우선)", example = "MENTOR")
        String role
) {
}
