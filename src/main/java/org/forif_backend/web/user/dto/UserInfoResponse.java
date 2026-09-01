package org.forif_backend.web.user.dto;

import lombok.Builder;

@Builder
public record UserInfoResponse(
        Long userId,
        String userName,
        String email,
        String phoneNum,
        String department,
        String imgUrl
) {
}
