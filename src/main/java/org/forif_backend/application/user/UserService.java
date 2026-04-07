package org.forif_backend.application.user;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.auth.RefreshTokenService;
import org.forif_backend.application.user.dto.*;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;
import org.forif_backend.domain.study.*;
import org.forif_backend.domain.user.GoogleOAuthClient;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.common.util.DateUtils;
import org.forif_backend.domain.user.*;
import org.forif_backend.web.user.dto.MemberResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserApplyRepository userApplyRepository;
    private final StudyRepository studyRepository;
    private final StudyUserRepository studyUserRepository;
    private final StaffAccountRepository staffAccountRepository;
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
        // Refresh Token 로테이션 (기존 토큰 무효화 + DB에서 현재 role 조회 + 새 토큰 발급)
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.rotateRefreshToken(command.refreshToken());

        return new RefreshTokenResult(tokenPair.accessToken(), tokenPair.refreshToken());
    }

    /**
     * 멘티 스터디 신청서 목록 조회
     */
    public GetStudyApplicationsResult getStudyApplications(Long userId) {
        List<UserApply> userApplies = userApplyRepository.findAllUserApplyByUserId(userId);

        // 모든 스터디 ID를 수집하여 배치 조회
        List<Integer> studyIds = userApplies.stream()
                .flatMap(ua -> {
                    List<Integer> ids = new ArrayList<>();
                    ids.add(ua.getPrimaryStudy());
                    if (ua.getSecondaryStudy() != null) {
                        ids.add(ua.getSecondaryStudy());
                    }
                    return ids.stream();
                })
                .distinct()
                .toList();

        Map<Integer, Study> studyMap = studyRepository.findStudiesByIdsWithTags(studyIds);

        List<StudyApplicationDto> applications = userApplies.stream()
                .map(userApply -> {
                    Study primaryStudy = studyMap.get(userApply.getPrimaryStudy());
                    ApplicationDetailDto primaryApplication = createApplicationDetailDto(
                            "PRIMARY", primaryStudy, userApply.getPrimaryStatus(), userApply.getPrimaryIntro()
                    );

                    ApplicationDetailDto secondaryApplication = null;
                    if (userApply.getSecondaryStudy() != null) {
                        Study secondaryStudy = studyMap.get(userApply.getSecondaryStudy());
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
        List<Study> studies = studyRepository.findAllStudiesByMentorId(userId);

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
                            .map(StudyTag::getName)
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
                            study.getActYear(),
                            study.getActSemester(),
                            role,
                            partnerMentorName,
                            study.getCreatedAt(),
                            study.getStudyStatus().getValue(),
                            study.getRejectReason()
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
                        .map(StudyTag::getName)
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

    /**
     * 인증서 조회
     */
    public GetCertificateResult getCertificate(Long userId, Integer studyId) {
        // 1. StudyUser 조회
        StudyUser studyUser = studyUserRepository.findByUserIdAndStudyId(userId, studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.CERTIFICATE_NOT_ISSUED));

        // 2. certificate_status 확인 (0: 미발급, 1: 발급)
        if (studyUser.getCertificateStatus() == null || studyUser.getCertificateStatus() == 0) {
            throw new ForifException(ErrorCode.CERTIFICATE_NOT_ISSUED);
        }

        // 3. certificateUrl 확인
        if (studyUser.getCertificateUrl() == null || studyUser.getCertificateUrl().isEmpty()) {
            throw new ForifException(ErrorCode.CERTIFICATE_NOT_ISSUED);
        }

        return new GetCertificateResult(studyUser.getCertificateUrl());
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @Transactional
    public User getUserInfo(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 전체 부원 목록 조회 (커서 기반 페이지네이션)
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<MemberResponse> getAllMembers(Long cursor, int size, String search) {
        List<User> users = userRepository.searchUsersWithCursor(cursor, size, search);
        long totalElements = userRepository.countUsers(search);

        boolean hasNext = users.size() > size;
        List<User> content = hasNext ? users.subList(0, size) : users;

        int currentYear = DateUtils.getCurrentYear();
        int currentSemester = DateUtils.getCurrentSemester();

        List<MemberResponse> responses = content.stream()
                .map(u -> buildMemberResponse(u, currentYear, currentSemester))
                .toList();

        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;

        return new CursorPageResponse<>(responses, nextCursor != null ? nextCursor.intValue() : null, hasNext, totalElements);
    }

    /**
     * 학기별 부원 목록 조회 (커서 기반 페이지네이션)
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<MemberResponse> getAllMembers(int year, int semester, Long cursor, int size, String search) {
        List<User> users = userRepository.searchUsersByYearSemester(year, semester, cursor, size, search);
        long totalElements = userRepository.countUsersByYearSemester(year, semester, search);

        boolean hasNext = users.size() > size;
        List<User> content = hasNext ? users.subList(0, size) : users;

        List<MemberResponse> responses = content.stream()
                .map(u -> buildMemberResponse(u, year, semester))
                .toList();

        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;

        return new CursorPageResponse<>(responses, nextCursor != null ? nextCursor.intValue() : null, hasNext, totalElements);
    }

    private MemberResponse buildMemberResponse(User u, int year, int semester) {
        List<Study> studies = studyRepository.findStudiesByUserId(u.getId());
        String studyName = studies.stream()
                .filter(s -> s.getActYear() == year && s.getActSemester() == semester)
                .map(Study::getStudyName)
                .findFirst()
                .orElse(null);

        Optional<StaffAccount> staffOpt = staffAccountRepository.findByUserId(u.getId());
        boolean isMentor = staffOpt.map(s -> s.getRole() == StaffRole.MENTOR).orElse(false);
        boolean isAdmin = staffOpt.map(s -> s.getRole() == StaffRole.ADMIN).orElse(false);

        return MemberResponse.builder()
                .userId(u.getId())
                .department(u.getDepartment())
                .userName(u.getUserName())
                .phoneNum(u.getPhoneNum())
                .currentStudyName(studyName)
                .isMentor(isMentor)
                .isAdmin(isAdmin)
                .build();
    }
}
