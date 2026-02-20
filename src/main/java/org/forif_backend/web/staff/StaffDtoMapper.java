package org.forif_backend.web.staff;

import org.forif_backend.application.staff.dto.CreateMentorCommand;
import org.forif_backend.application.staff.dto.StaffSignInCommand;
import org.forif_backend.application.staff.dto.StaffSignInResult;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.user.User;
import org.forif_backend.web.staff.dto.CreateMentorRequest;
import org.forif_backend.web.staff.dto.StaffInfoResponse;
import org.forif_backend.web.staff.dto.StaffSignInRequest;
import org.forif_backend.web.staff.dto.StaffSignInResponse;

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

    public static CreateMentorCommand toCommand(CreateMentorRequest request) {
        return new CreateMentorCommand(
            request.userId(),
            request.password(),
            request.affiliation()
        );
    }

    public static CreateAdminCommand toCommand(CreateAdminRequest request) {
        return new CreateAdminCommand(
            request.userId(),
            request.password(),
            request.affiliation()
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

    public static StaffInfoResponse toResponse(StaffAccount staffAccount) {
        User user = staffAccount.getUser();
        return StaffInfoResponse.builder()
                .userId(staffAccount.getUserId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .phoneNum(user.getPhoneNum())
                .department(user.getDepartment())
                .imgUrl(user.getImgUrl())
                .role(staffAccount.getRole().getValue())
                .build();
    }

    public static AdminResponse toAdminResponse(StaffAccount staffAccount) {
        return AdminResponse.builder()
                .userId(staffAccount.getUserId())
                .name(staffAccount.getName())
                .affiliation(staffAccount.getAffiliation())
                .build();
    }
}
