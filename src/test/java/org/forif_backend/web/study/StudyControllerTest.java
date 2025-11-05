package org.forif_backend.web.study;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.forif_backend.application.study.StudyService;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyDifficulty;
import org.forif_backend.domain.study.RecruitStatus;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudyController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudyService studyService;

    @Test
    @DisplayName("GET /api/v1/studies - 정상 요청")
    void getStudies_success() throws Exception {
        // given
        Study study1 = new Study();
        study1.setId(1);
        study1.setStudyName("Spring Boot 스터디");
        study1.setPrimaryMentorName("김멘토");
        study1.setDifficulty(StudyDifficulty.EASY);
        study1.setRecruitStatus(RecruitStatus.APPLICABLE);
        study1.setTags(new ArrayList<>());

        Study study2 = new Study();
        study2.setId(2);
        study2.setStudyName("React 심화");
        study2.setPrimaryMentorName("이멘토");
        study2.setDifficulty(StudyDifficulty.NORMAL);
        study2.setRecruitStatus(RecruitStatus.CLOSED);
        study2.setTags(new ArrayList<>());

        List<Study> studies = Arrays.asList(study1, study2);

        when(studyService.getStudies(anyLong(), anyLong(), any(), any(), any(), any(), any(), any()))
                .thenReturn(studies);

        // when & then
        mockMvc.perform(get("/api/v1/studies")
                .param("page", "0")
                .param("page_size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.studies[0].study_name").value("Spring Boot 스터디"));
    }

    @Test
    @DisplayName("GET /api/v1/studies - 모든 파라미터 포함 요청")
    void getStudies_withAllParameters() throws Exception {
        // given
        List<Study> studies = new ArrayList<>();

        when(studyService.getStudies(eq(0L), eq(10L), eq(2024), eq(2),
                any(List.class), any(List.class), any(RecruitStatus.class), eq("spring")))
                .thenReturn(studies);

        // when & then
        mockMvc.perform(get("/api/v1/studies")
                .param("page", "0")
                .param("page_size", "10")
                .param("year", "2024")
                .param("semester", "2")
                .param("difficulties", "EASY", "NORMAL")
                .param("tags", "Spring", "Backend")
                .param("recruit_status", "APPLICABLE")
                .param("search", "spring"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/studies - 잘못된 difficulty 값")
    void getStudies_invalidDifficulty() throws Exception {
        mockMvc.perform(get("/api/v1/studies")
                .param("page", "0")
                .param("page_size", "10")
                .param("difficulties", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/studies - 잘못된 recruitStatus 값")
    void getStudies_invalidRecruitStatus() throws Exception {
        mockMvc.perform(get("/api/v1/studies")
                .param("page", "0")
                .param("page_size", "10")
                .param("recruit_status", "invalid"))
                .andExpect(status().isBadRequest());
    }
}
