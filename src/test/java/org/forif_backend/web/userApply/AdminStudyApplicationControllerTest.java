package org.forif_backend.web.userApply;

import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.user.UserApplyService;
import org.forif_backend.application.user.dto.AdminStudyApplicationInfo;
import org.forif_backend.common.auth.JwtAuthenticationFilter;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.common.type.SortCriteria;
import org.forif_backend.common.type.SortDirection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminStudyApplicationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AdminStudyApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserApplyService userApplyService;

    @MockitoBean
    private SemesterService semesterService;

    @Test
    void forwardsUrlSortConditionsToTheApplicationListService() throws Exception {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        AdminStudyApplicationInfo row = new AdminStudyApplicationInfo(
                20260001L, "신청자", "컴퓨터학부", "웹 스터디", 1,
                LocalDateTime.of(2026, 8, 1, 12, 0));
        when(userApplyService.getAdminApplications(
                2026, 2, 1, 10, "웹", List.of(
                        new SortCriteria("userName", SortDirection.ASC),
                        new SortCriteria("appliedAt", SortDirection.DESC))))
                .thenReturn(CursorPageResponse.ofOffset(List.of(row), false, 1, 1, 10));

        mockMvc.perform(get("/api/v1/admin/study-applications")
                        .param("page", "1")
                        .param("size", "10")
                        .param("search", "웹")
                        .param("sort", "userName:asc", "appliedAt:desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].study_name").value("웹 스터디"));

        verify(userApplyService).getAdminApplications(
                2026, 2, 1, 10, "웹", List.of(
                        new SortCriteria("userName", SortDirection.ASC),
                        new SortCriteria("appliedAt", SortDirection.DESC)));
    }
}
