package org.forif_backend.web.staff;

import org.forif_backend.application.staff.dto.*;
import org.forif_backend.web.staff.dto.*;

/**
 * Staff Web DTO ↔ Application DTO 변환
 */
public class StaffDtoMapper {

    /**
     * Web DTO → Application Command
     */
    public static StaffSignInCommand toCommand(StaffSignInRequest request) {
        return new StaffSignInCommand(
            request.userId(),
            request.password()
        );
    }

    /**
     * Application Result → Web DTO
     */
    public static StaffSignInResponse toResponse(StaffSignInResult result) {
        return StaffSignInResponse.builder()
            .accessToken(result.accessToken())
            .role(result.role())
            .build();
    }
}
