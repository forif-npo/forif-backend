package org.forif_backend.application.user;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.user.dto.*;
import org.forif_backend.common.auth.JwtProvider;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

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
        if (userRepository.findById(command.studentId()).isPresent()) {
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
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
            .responseTimeout(Duration.ofMillis(5000))
            .doOnConnected(conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(3000, TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(3000, TimeUnit.MILLISECONDS)));

        WebClient webClient = WebClient.builder()
            .baseUrl("https://www.googleapis.com/oauth2/v3/userinfo")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();

        try {
            GoogleUserInfo response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .queryParam("access_token", token)
                    .build())
                .retrieve()
                .bodyToMono(GoogleUserInfo.class)
                .block();

                  log.info("Google user email retrieved successfully: {}", response.email());
                  return response.email();
        } catch (WebClientResponseException e) {
            log.error("Error while retrieving user email: {}", e.getResponseBodyAsString(), e);
            throw new ForifException(ErrorCode.BAD_REQUEST, "유효하지 않은 Google 토큰입니다.");
        } catch (Exception e) {
            log.error("Unexpected error while retrieving user email", e);
            throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR, "사용자 이메일을 가져오는 중 오류가 발생했습니다.");
        }
    }

    /**
     * Refresh Token으로 새로운 Access Token 발급
     */
    public RefreshTokenResult refreshAccessToken(RefreshTokenCommand command) {
        // 1. Refresh Token 유효성 검증
        if (!jwtProvider.validateToken(command.refreshToken())) {
            throw new ForifException(ErrorCode.BAD_REQUEST, "유효하지 않은 Refresh Token입니다.");
        }

        // 2. Refresh Token 만료 확인
        if (jwtProvider.isExpired(command.refreshToken())) {
            throw new ForifException(ErrorCode.BAD_REQUEST, "Refresh Token이 만료되었습니다. 다시 로그인해주세요.");
        }

        // 3. 토큰에서 사용자 ID 추출
        String userId = jwtProvider.getUserIdFromToken(command.refreshToken());

        // 4. 사용자 존재 여부 확인
        if (!userRepository.findById(Long.parseLong(userId)).isPresent()) {
            throw new ForifException(ErrorCode.USER_NOT_FOUND, "존재하지 않는 사용자입니다.");
        }

        // 5. 새로운 Access Token 발급
        String newAccessToken = jwtProvider.generateAccessToken(userId);

        return new RefreshTokenResult(newAccessToken);
    }
}
