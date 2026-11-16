package org.forif_backend.web.staffrecruitment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.forif_backend.application.staffrecruitment.StaffApplicationService;
import org.forif_backend.application.staffrecruitment.dto.MyStaffApplicationSummary;
import org.forif_backend.domain.staffrecruitment.StaffApplicationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MyStaffApplicationControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 로그인한_회원의_지원_이력만_요청하고_개인정보_없이_반환한다() throws Exception {
        Long userId = 123L;
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
        StaffApplicationService service = mock(StaffApplicationService.class);
        when(service.getMyHistory(userId)).thenReturn(List.of(new MyStaffApplicationSummary(
                7, 12, 2027, 1, StaffApplicationStatus.SUBMITTED,
                LocalDateTime.of(2026, 11, 17, 12, 0))));
        var mapper = new ObjectMapper().findAndRegisterModules()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        var mockMvc = MockMvcBuilders.standaloneSetup(new MyStaffApplicationController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();

        mockMvc.perform(get("/api/v1/staff-applications/me"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, private"))
                .andExpect(jsonPath("$.data[0].application_id").value(7))
                .andExpect(jsonPath("$.data[0].recruitment_id").value(12))
                .andExpect(jsonPath("$.data[0].act_year").value(2027))
                .andExpect(jsonPath("$.data[0].applicant_email").doesNotExist());
        verify(service).getMyHistory(userId);
    }
}
