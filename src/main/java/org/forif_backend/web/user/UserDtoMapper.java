package org.forif_backend.web.user;

import org.forif_backend.application.user.dto.*;
import org.forif_backend.application.study.dto.SemesterStudiesInfo;
import org.forif_backend.application.study.dto.StudyInfo;
import org.forif_backend.application.study.dto.UserStudiesResult;
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

    public static UserStudiesResponse toResponse(UserStudiesResult result) {
        return UserStudiesResponse.builder()
                .semesters(result.semesters().stream()
                        .map(UserDtoMapper::toSemesterStudies)
                        .toList())
                .build();
    }

    private static UserStudiesResponse.SemesterStudies toSemesterStudies(SemesterStudiesInfo info) {
        return UserStudiesResponse.SemesterStudies.builder()
                .year(info.year())
                .semester(info.semester())
                .semesterLabel(info.semesterLabel())
                .isCurrent(info.isCurrent())
                .study(info.study() != null ? toStudyDetail(info.study()) : null)
                .build();
    }

    private static UserStudiesResponse.StudyDetail toStudyDetail(StudyInfo info) {
        return UserStudiesResponse.StudyDetail.builder()
                .studyId(info.studyId())
                .studyName(info.studyName())
                .primaryMentorName(info.primaryMentorName())
                .secondaryMentorName(info.secondaryMentorName())
                .tags(info.tags())
                .oneLiner(info.oneLiner())
                .startTime(info.startTime())
                .endTime(info.endTime())
                .weekDay(info.weekDay())
                .location(info.location())
                .difficulty(info.difficulty())
                .imgUrl(info.imgUrl())
                .build();
    }

    public static StudyApplicationsResponse toResponse(GetStudyApplicationsResult result) {
        return new StudyApplicationsResponse(result.applications());
    }

    public static StudyCreationApplicationsResponse toResponse(GetStudyCreationApplicationsResult result) {
        return new StudyCreationApplicationsResponse(result.applications());
    }

    public static CertificateResponse toResponse(GetCertificateResult result) {
        return CertificateResponse.builder()
            .certificateUrl(result.certificateUrl())
            .build();
    }
}
