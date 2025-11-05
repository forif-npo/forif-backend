package org.forif_backend.application.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.study.StudyService;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.common.util.DateUtils;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.user.dto.*;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.user.GoogleOAuthClient;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.web.user.dto.StudyApplyRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final GoogleOAuthClient googleOAuthClient;
    private final StudyRepository studyRepository;

    /**
     * 부원 회원가입
     */
    @Transactional
    public UserSignUpResult userSignUp(UserSignUpCommand command) {
        // 1. 한양대 이메일 도메인 검증
        if (!command.email().endsWith("@hanyang.ac.kr")) {
            throw new ForifException(ErrorCode.BAD_REQUEST, "한양대 이메일(@hanyang.ac.kr)만 가입 가능합니다.");
        }

        // 2. 중복 확인
        if (userRepository.existsById(command.studentId())) {
            throw new ForifException(ErrorCode.BAD_REQUEST, "이미 가입된 학번입니다.");
        }
        if (userRepository.existsByEmail(command.email())) {
            throw new ForifException(ErrorCode.BAD_REQUEST, "이미 가입된 이메일입니다.");
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

        return new UserSignUpResult(
            savedUser.getId(),
            savedUser.getUserName(),
            savedUser.getEmail()
        );
    }

    /**
     * 부원 로그인
     */
    public UserSignInResult userSignIn(UserSignInCommand command) {
        // 1. 한양대 이메일 도메인 검증
        if (!command.email().endsWith("@hanyang.ac.kr")) {
            throw new ForifException(ErrorCode.BAD_REQUEST, "한양대 이메일(@hanyang.ac.kr)만 로그인 가능합니다.");
        }

        // 2. 기존 사용자 조회
        User user = userRepository.findByEmail(command.email())
            .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND, "등록되지 않은 사용자입니다. 먼저 회원가입을 진행해주세요."));

        // 3. JWT 토큰 생성
        String accessToken = jwtProvider.generateAccessToken(user.getId().toString());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId().toString());

        return new UserSignInResult(
            accessToken,
            refreshToken,
            user.getId(),
            user.getUserName()
        );
    }


    /**
     * Google OAuth Access Token으로 사용자 이메일 조회
     */
    public String getEmailFromGoogleToken(String token) {
        return googleOAuthClient.getEmailFromToken(token);
    }

    /**
     * Refresh Token으로 새로운 Access Token 발급
     */
    public RefreshTokenResult refreshAccessToken(RefreshTokenCommand command) {
        // 1. Refresh Token 유효성 및 만료 검증
        if (!jwtProvider.validateToken(command.refreshToken())) {
            throw new ForifException(ErrorCode.BAD_REQUEST, "유효하지 않거나 만료된 Refresh Token입니다.");
        }

        // 2. 토큰에서 사용자 ID 추출
        String userId = jwtProvider.getUserIdFromToken(command.refreshToken());

        // 3. 사용자 존재 여부 확인
        if (!userRepository.findById(Long.parseLong(userId)).isPresent()) {
            throw new ForifException(ErrorCode.USER_NOT_FOUND, "존재하지 않는 사용자입니다.");
        }

        // 4. 새로운 Access Token 발급
        String newAccessToken = jwtProvider.generateAccessToken(userId);

        return new RefreshTokenResult(newAccessToken);
    }

    /**
     * 스터디 지원 메서드
     * @param userId 유저ID
     * @param request 요청 dto
     */
    public void applyStudy(Long userId, StudyApplyRequest request) {
        // 유저 조회
        User user = userRepository.findById(userId).orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        // 이번 학기에 지원한 스터디 있는지 확인
        if(userRepository.existUserApply(DateUtils.getCurrentYear(), DateUtils.getCurrentSemester(), user)) {
            throw new ForifException(ErrorCode.USER_APPLY_ALREADY_EXISTS);
        }

        // 지원 스터디 존재 확인
        studyRepository.findStudyById(request.primaryStudyId())
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));
        Optional.ofNullable(request.secondaryStudyId()).ifPresent(secondaryStudyId -> studyRepository.findStudyById(secondaryStudyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND)));

        // 지원 정보 생성
        UserApply userApply = UserApply.applyStudy(request, user);

        // 지원
        userRepository.createUserApply(userApply);
    }
}
