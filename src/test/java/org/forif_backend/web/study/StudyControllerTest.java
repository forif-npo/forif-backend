package org.forif_backend.web.study;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.forif_backend.application.study.dto.StudyDto;
import org.forif_backend.application.study.dto.StudyTagDto;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.forif_backend.application.study.StudyService;
import org.forif_backend.domain.study.StudyDifficulty;
import org.forif_backend.domain.study.RecruitStatus;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudyController.class)
@Disabled
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
        StudyDto study1 = StudyDto.builder()
                .id(1)
                .studyName("Spring Boot 스터디")
                .primaryMentorName("김멘토")
                .difficulty(StudyDifficulty.EASY)
                .recruitStatus(RecruitStatus.APPLICABLE)
                .tags(new ArrayList<>())
                .actYear(2024)
                .actSemester(1)
                .build();

        StudyDto study2 = StudyDto.builder()
                .id(2)
                .studyName("React 심화")
                .primaryMentorName("이멘토")
                .difficulty(StudyDifficulty.NORMAL)
                .recruitStatus(RecruitStatus.CLOSED)
                .tags(new ArrayList<>())
                .actYear(2024)
                .actSemester(1)
                .build();

        List<StudyDto> studies = Arrays.asList(study1, study2);
        CursorPageResponse<StudyDto> cursorResponse = CursorPageResponse.ofCursor(studies, null, false, 2);

        when(studyService.getStudies(any(), any(), anyInt(), any(), any(), any(), any(), any(), any()))
                .thenReturn(cursorResponse);

        // when & then
        mockMvc.perform(get("/api/v1/studies")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].study_name").value("Spring Boot 스터디"))
                .andExpect(jsonPath("$.data.content[0].primary_mentor_name").value("김멘토"))
                .andExpect(jsonPath("$.data.content[0].difficulty").value("EASY"))
                .andExpect(jsonPath("$.data.content[0].recruit_status").value("APPLICABLE"))
                .andExpect(jsonPath("$.data.content[0].tags").isEmpty())
                .andExpect(jsonPath("$.data.content[1].study_name").value("React 심화"))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/studies - 모든 파라미터 포함 요청")
    void getStudies_withAllParameters() throws Exception {
        // given
        CursorPageResponse<StudyDto> cursorResponse = CursorPageResponse.ofCursor(new ArrayList<>(), null, false, 0);

        when(studyService.getStudies(any(), any(), anyInt(), eq(2024), eq(2),
                any(List.class), any(List.class), any(RecruitStatus.class), eq("spring")))
                .thenReturn(cursorResponse);

        // when & then
        mockMvc.perform(get("/api/v1/studies")
                        .param("size", "10")
                        .param("year", "2024")
                        .param("semester", "2")
                        .param("difficulties", "EASY", "NORMAL")
                        .param("tags", "Spring", "Backend")
                        .param("recruit_status", "APPLICABLE")
                        .param("search", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/studies - 잘못된 difficulty 값")
    void getStudies_invalidDifficulty() throws Exception {
        mockMvc.perform(get("/api/v1/studies")
                        .param("size", "10")
                        .param("difficulties", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/studies - 잘못된 recruitStatus 값")
    void getStudies_invalidRecruitStatus() throws Exception {
        mockMvc.perform(get("/api/v1/studies")
                        .param("size", "10")
                        .param("recruit_status", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/studies/me/created - 내가 개설한 스터디 조회")
    void getMyCreatedStudies_success() throws Exception {
        // given
        StudyDto study1 = StudyDto.builder()
                .id(1)
                .studyName("내 스터디 1")
                .primaryMentorName("나")
                .difficulty(StudyDifficulty.EASY)
                .recruitStatus(RecruitStatus.APPLICABLE)
                .tags(Arrays.asList(
                        StudyTagDto.builder().id(1L).name("Java").build(),
                        StudyTagDto.builder().id(2L).name("Spring").build()
                ))
                .actYear(2024)
                .actSemester(2)
                .build();

        List<StudyDto> studies = Collections.singletonList(study1);

        when(studyService.getMyCreatedStudies(anyLong()))
                .thenReturn(studies);

        // when & then
        mockMvc.perform(get("/api/v1/studies/me/created")
                        .param("mentorId", "1"))  // 임시 (나중에 @AuthenticationPrincipal로 변경)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].study_name").value("내 스터디 1"))
                .andExpect(jsonPath("$.data[0].tags").isArray())
                .andExpect(jsonPath("$.data[0].tags[0]").value("Java"))
                .andExpect(jsonPath("$.data[0].tags[1]").value("Spring"));
    }
}