package org.forif_backend.web.user;

import org.forif_backend.application.user.dto.*;
import org.forif_backend.web.user.dto.*;

/**
 * User Web DTO ↔ Application DTO 변환
 */
public class UserDtoMapper {

    /**
     * Web DTO → Application Command
     */
    public static UserSignUpCommand toCommand(UserSignUpRequest request) {
        return new UserSignUpCommand(
            request.studentId(),
            request.userName(),
            request.email(),
            request.phoneNum(),
            request.department()
        );
    }

    public static UserSignInCommand toCommand(String email) {
        return new UserSignInCommand(email);
    }

    public static RefreshTokenCommand toCommand(RefreshTokenRequest request) {
        return new RefreshTokenCommand(request.refreshToken());
    }

    /**
     * Application Result → Web DTO
     */
    public static UserSignUpResponse toResponse(UserSignUpResult result) {
        return UserSignUpResponse.builder()
            .accessToken(result.accessToken())
            .role(result.role())
            .build();
    }

    public static UserSignInResponse toResponse(UserSignInResult result) {
        return UserSignInResponse.builder()
            .accessToken(result.accessToken())
            .role(result.role())
            .build();
    }

    public static AccessTokenResponse toResponse(RefreshTokenResult result) {
        return AccessTokenResponse.builder()
            .accessToken(result.accessToken())
            .build();
    }

    public static StudyApplicationsResponse toResponse(GetStudyApplicationsResult result) {
        return new StudyApplicationsResponse(result.applications());
    }

    public static StudyCreationApplicationsResponse toResponse(GetStudyCreationApplicationsResult result) {
        return new StudyCreationApplicationsResponse(result.applications());
    }
}
