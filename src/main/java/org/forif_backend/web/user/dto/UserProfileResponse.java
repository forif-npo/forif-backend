package org.forif_backend.web.user.dto;

import org.forif_backend.domain.user.User;

public record UserProfileResponse(
        Long userId,
        String userName,
        String email,
        String phoneNum,
        String department,
        String imgUrl
) {
    public static UserProfileResponse from(User user, String imgUrl) {
        return new UserProfileResponse(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getPhoneNum(),
                user.getDepartment(),
                imgUrl
        );
    }
}
