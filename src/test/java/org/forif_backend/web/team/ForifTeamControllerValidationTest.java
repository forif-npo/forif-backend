package org.forif_backend.web.team;

import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.application.team.ForifTeamService;
import org.forif_backend.common.auth.JwtAuthenticationFilter;
import org.forif_backend.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ForifTeamController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class ForifTeamControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ForifTeamService forifTeamService;

    @MockitoBean
    private StaffAccountService staffAccountService;

    @Test
    void rejectsFieldsThatExceedTheirDatabaseColumnLengths() throws Exception {
        String selfIntro = "a".repeat(101);
        String request = """
                {"user_title":"%s","club_department":"%s","intro_tag":"%s","self_intro":"%s"}
                """.formatted("a".repeat(31), "a".repeat(31), "a".repeat(101), selfIntro);

        mockMvc.perform(patch("/api/v1/admin/forif-team/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value(ErrorCode.VALIDATION_FAILED.getCode()));

        verifyNoInteractions(forifTeamService);
    }
}
