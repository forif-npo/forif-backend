package org.forif_backend.web.userApply;

import org.forif_backend.application.user.UserApplyService;
import org.forif_backend.common.auth.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserApplyController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class UserApplyControllerTest {

    private static final long APPLICATION_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserApplyService userApplyService;

    @Test
    void cancelsRequestedPriority() throws Exception {
        mockMvc.perform(delete("/api/v1/users/apply/{applyId}", APPLICATION_ID)
                        .param("priority", "2"))
                .andExpect(status().isOk());

        verify(userApplyService).cancelApplication(null, APPLICATION_ID, 2);
    }

    @Test
    void rejectsRequestWithoutPriority() throws Exception {
        mockMvc.perform(delete("/api/v1/users/apply/{applyId}", APPLICATION_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("FOR004-400"));
    }
}
