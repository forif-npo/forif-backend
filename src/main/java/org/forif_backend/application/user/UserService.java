package org.forif_backend.application.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.auth.RefreshTokenService;
import org.forif_backend.application.user.dto.*;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.user.GoogleOAuthClient;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.common.util.DateUtils;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.user.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserApplyRepository userApplyRepository;
    private final StudyRepository studyRepository;
    private final JwtProvider jwtProvider;
    private final GoogleOAuthClient googleOAuthClient;
    private final RefreshTokenService refreshTokenService;

    /**
     * 부원 회원가입
     */
    @Transactional
    public UserSignUpResult userSignUp(UserSignUpCommand command) {
        // 1. 한양대 이메일 도메인 검증
        if (!command.email().endsWith("@hanyang.ac.kr")) {
            throw new ForifException(ErrorCode.INVALID_EMAIL_DOMAIN);
        }

        // 2. 중복 확인
        if (userRepository.existsById(command.studentId())) {
            throw new ForifException(ErrorCode.STUDENT_ID_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(command.email())) {
            throw new ForifException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 3. 사용자 생성
        User user = User.createUser(
                command.studentId(),
                command.userName(),
                command.email(),
                command.phoneNum(),
                command.department()
        );

        User savedUser = userRepository.save(user);

        // 4. JWT 토큰 생성
        String role = "USER";
        String userId = savedUser.getId().toString();
        String accessToken = jwtProvider.generateAccessToken(userId, role);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        // 5. Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(userId, refreshToken);

        return new UserSignUpResult(
                accessToken,
                refreshToken,
                role
        );
    }


    /**
     * 부원 로그인
     */
    public UserSignInResult userSignIn(UserSignInCommand command) {
        // 1. 한양대 이메일 도메인 검증
        if (!command.email().endsWith("@hanyang.ac.kr")) {
            throw new ForifException(ErrorCode.INVALID_EMAIL_DOMAIN);
        }


        // 2. 기존 사용자 조회
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        // 3. JWT 토큰 생성
        String role = "USER";
        String userId = user.getId().toString();
        String accessToken = jwtProvider.generateAccessToken(userId, role);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        // 4. Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(userId, refreshToken);


        return new UserSignInResult(
                accessToken,
                refreshToken,
                role
        );
    }


    /**
     * Google OAuth Access Token으로 사용자 이메일 조회
     */
    public String getEmailFromGoogleToken(String token) {
        return googleOAuthClient.getEmailFromToken(token);
    }

    /**
     * Refresh Token으로 새로운 Access Token 발급 (토큰 로테이션 적용)
     */
    public RefreshTokenResult refreshAccessToken(RefreshTokenCommand command) {
        // 1. 토큰에서 role 추출 (로테이션 전)
        String role = jwtProvider.getRoleFromToken(command.refreshToken());

        // 2. Refresh Token 로테이션 (기존 토큰 무효화 + 새 토큰 발급)
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.rotateRefreshToken(command.refreshToken(), role);

        return new RefreshTokenResult(tokenPair.accessToken(), tokenPair.refreshToken());
    }

    /**
     * 멘티 스터디 신청서 목록 조회
     */
    public GetStudyApplicationsResult getStudyApplications(Long userId) {
        List<UserApply> userApplies = userApplyRepository.findAllUserApplyByUserId(userId);

        List<StudyApplicationDto> applications = userApplies.stream()
                .map(userApply -> {
                    Study primaryStudy = studyRepository.findStudyByIdWithTags(userApply.getPrimaryStudy())
                            .orElse(null);
                    ApplicationDetailDto primaryApplication = createApplicationDetailDto(
                            "PRIMARY", primaryStudy, userApply.getPrimaryStatus(), userApply.getPrimaryIntro()
                    );

                    ApplicationDetailDto secondaryApplication = null;
                    if (userApply.getSecondaryStudy() != null) {
                        Study secondaryStudy = studyRepository.findStudyByIdWithTags(userApply.getSecondaryStudy())
                                .orElse(null);
                        secondaryApplication = createApplicationDetailDto(
                                "SECONDARY", secondaryStudy, userApply.getSecondaryStatus(), userApply.getSecondaryIntro()
                        );
                    }

                    return new StudyApplicationDto(
                            userApply.getId(),
                            userApply.getApplyYear(),
                            userApply.getApplySemester(),
                            userApply.getCreatedAt().toLocalDate(),
                            userApply.getApplier().getDepartment(),
                            userApply.getPayStatus(),
                            primaryApplication,
                            secondaryApplication
                    );
                })
                .collect(Collectors.toList());

        return new GetStudyApplicationsResult(applications);
    }

    /**
     * 멘토 스터디 개설 신청서 목록 조회
     */
    public GetStudyCreationApplicationsResult getStudyCreationApplications(Long userId) {
        List<Study> studies = studyRepository.findAllStudiesByMentorIdAndIsApplied(userId, true);

        List<StudyCreationApplicationDto> applications = studies.stream()
                .map(study -> {
                    boolean isPrimaryMentor = study.getPrimaryMentor() != null &&
                            study.getPrimaryMentor().getId().equals(userId);
                    String role = isPrimaryMentor ? "PRIMARY_MENTOR" : "SECONDARY_MENTOR";

                    String partnerMentorName = null;
                    if (isPrimaryMentor && study.getSecondaryMentor() != null) {
                        partnerMentorName = study.getSecondaryMentor().getUserName();
                    } else if (!isPrimaryMentor && study.getPrimaryMentor() != null) {
                        partnerMentorName = study.getPrimaryMentor().getUserName();
                    }

                    List<String> tags = study.getTags().stream()
                            .map(tag -> tag.getName())
                            .collect(Collectors.toList());

                    return new StudyCreationApplicationDto(
                            study.getId(),
                            study.getStudyName(),
                            tags,
                            study.getSubTitle(),
                            study.getExplanation(),
                            study.getStartTime(),
                            study.getEndTime(),
                            study.getWeekDay(),
                            study.getLocation(),
                            study.getDifficulty() != null ? study.getDifficulty().ordinal() : null,
                            study.getIsApplied(),
                            study.getActYear(),
                            study.getActSemester(),
                            role,
                            partnerMentorName,
                            study.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());

        return new GetStudyCreationApplicationsResult(applications);
    }

    private ApplicationDetailDto createApplicationDetailDto(String priority, Study study, UserApplyStatus status, String intro) {
        if (study == null) {
            return null;
        }

        StudyInfoDto studyInfo = new StudyInfoDto(
                study.getId(),
                study.getStudyName(),
                study.getPrimaryMentorName(),
                study.getSecondaryMentorName(),
                study.getTags().stream()
                        .map(tag -> tag.getName())
                        .collect(Collectors.toList()),
                study.getOneLiner(),
                study.getWeekDay(),
                study.getStartTime(),
                study.getEndTime(),
                study.getLocation(),
                study.getDifficulty() != null ? study.getDifficulty().ordinal() : null,
                study.getImgUrl()
        );

        Integer statusValue = status != null ? status.ordinal() : null;

        return new ApplicationDetailDto(priority, studyInfo, statusValue, intro);
    }
}
