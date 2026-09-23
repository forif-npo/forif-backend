package org.forif_backend.web.staff;

import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.application.user.UserService;
import org.forif_backend.application.user.dto.ReturningMemberRoster;
import org.forif_backend.common.auth.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StaffAccountController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@Import(ReturningMemberRosterControllerTest.Security.class)
class ReturningMemberRosterControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean StaffAccountService staffAccountService;
    @MockitoBean UserService userService;

    @TestConfiguration
    @EnableMethodSecurity
    static class Security {
        @Bean
        SecurityFilterChain testSecurity(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(basic -> {}).build();
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void returnsTheActiveRosterWithSnakeCaseFieldsAndTextStudentId() throws Exception {
        when(userService.getReturningMemberRoster()).thenReturn(new ReturningMemberRoster(2026, 1, List.of(
                new ReturningMemberRoster.Member("910001", "가상부원", "가상대학", "가상학과", "01000000000"))));
        mvc.perform(get("/api/v1/admin/users/returning-members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.act_year").value(2026))
                .andExpect(jsonPath("$.data.act_semester").value(1))
                .andExpect(jsonPath("$.data.members[0].user_id").value("910001"))
                .andExpect(jsonPath("$.data.members[0].user_id").isString())
                .andExpect(jsonPath("$.data.members[0].user_name").value("가상부원"))
                .andExpect(jsonPath("$.data.members[0].college").value("가상대학"))
                .andExpect(jsonPath("$.data.members[0].department").value("가상학과"))
                .andExpect(jsonPath("$.data.members[0].phone_num").value("01000000000"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void rejectsNonAdminAccounts() throws Exception {
        mvc.perform(get("/api/v1/admin/users/returning-members")).andExpect(status().isForbidden());
        verifyNoInteractions(userService);
    }

    @Test
    void rejectsAnonymousRequests() throws Exception {
        mvc.perform(get("/api/v1/admin/users/returning-members").accept("application/json"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
    }
}
