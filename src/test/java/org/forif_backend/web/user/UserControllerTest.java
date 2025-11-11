//  ./gradlew test --tests UserControllerTest

package org.forif_backend.web.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forif_backend.application.user.UserService;
import org.forif_backend.web.user.dto.UserSignInRequest;
import org.forif_backend.web.user.dto.UserSignUpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Disabled("Redis and Google OAuth mocking required - temporarily disabled for CI/CD")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("회원가입 성공 테스트")
    void userSignUpSuccess() throws Exception {
        // given
        UserSignUpRequest request = new UserSignUpRequest(
                20241234L,
                "테스트유저",
                "test@hanyang.ac.kr",
                "010-1234-5678",
                "컴퓨터공학과"
        );

        // when & then
        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.error_code").doesNotExist())
                .andExpect(jsonPath("$.data.access_token").exists())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/"));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 이메일")
    void userSignUpFailDuplicateEmail() throws Exception {
        // given - 먼저 회원가입
        UserSignUpRequest firstRequest = new UserSignUpRequest(
                20241234L,
                "테스트유저1",
                "duplicate@hanyang.ac.kr",
                "010-1234-5678",
                "컴퓨터공학과"
        );

        mockMvc.perform(post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)));

        // when - 동일한 이메일로 다시 회원가입 시도
        UserSignUpRequest duplicateRequest = new UserSignUpRequest(
                20245678L,
                "테스트유저2",
                "duplicate@hanyang.ac.kr",
                "010-9876-5432",
                "전자공학과"
        );

        // then
        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.error_code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @Disabled("Google OAuth API mocking required")
    @DisplayName("로그인 성공 테스트 (Google OAuth 없이 이메일로)")
    void userSignInSuccess() throws Exception {
        // given - 먼저 회원가입
        UserSignUpRequest signUpRequest = new UserSignUpRequest(
                20241234L,
                "로그인테스트",
                "signin@hanyang.ac.kr",
                "010-1234-5678",
                "컴퓨터공학과"
        );

        mockMvc.perform(post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)));

    }

    @Test
    @DisplayName("Refresh Token으로 Access Token 재발급 테스트")
    void refreshAccessTokenSuccess() throws Exception {
        // given - 먼저 회원가입하여 토큰 받기
        UserSignUpRequest signUpRequest = new UserSignUpRequest(
                20241234L,
                "리프레시테스트",
                "refresh@hanyang.ac.kr",
                "010-1234-5678",
                "컴퓨터공학과"
        );

        var signUpResult = mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andReturn();

        String refreshToken = signUpResult.getResponse().getCookie("refreshToken").getValue();

        // when & then - Refresh Token으로 Access Token 재발급
        mockMvc.perform(post("/api/v1/users/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.access_token").exists());
    }

    @Test
    @DisplayName("Refresh Token 없이 재발급 요청 - 실패")
    void refreshAccessTokenFailWithoutToken() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/users/refresh"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("FOR013-401"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("로그아웃 테스트")
    void logoutSuccess() throws Exception {
        // given - 먼저 회원가입하여 토큰 받기
        UserSignUpRequest signUpRequest = new UserSignUpRequest(
                20241234L,
                "로그아웃테스트",
                "logout@hanyang.ac.kr",
                "010-1234-5678",
                "컴퓨터공학과"
        );

        var signUpResult = mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andReturn();

        String responseBody = signUpResult.getResponse().getContentAsString();
        String accessToken = com.jayway.jsonpath.JsonPath.read(responseBody, "$.data.access_token");

        // when & then - 로그아웃 (인증 토큰 포함)
        mockMvc.perform(post("/api/v1/users/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().maxAge("refreshToken", 0)); // 쿠키 만료 확인
    }
}
