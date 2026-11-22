package org.forif_backend.web.staffrecruitment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.forif_backend.application.staffrecruitment.StaffApplicationService;
import org.forif_backend.application.staffrecruitment.dto.StaffApplicationDetail;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staffrecruitment.StaffApplicationStatus;
import org.forif_backend.web.staffrecruitment.dto.StaffApplicationRequests;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StaffApplicationControllerMultipartTest {

    private final StaffApplicationService service = mock(StaffApplicationService.class);
    private final StaffApplicationController controller = new StaffApplicationController(service);
    private final StaffApplicationRequests.SubmitRequest request = new StaffApplicationRequests.SubmitRequest(
            List.of(new StaffApplicationRequests.ChoiceRequest(31, 1)),
            List.of(new StaffApplicationRequests.AnswerRequest(41, "")),
            List.of());

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void HTTP_multipart_JSON과_파일_파트를_정상_바인딩한다() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of()));
        var mapper = new ObjectMapper().findAndRegisterModules()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        var mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
        when(service.submit(eq(12), eq(1L), any(), any())).thenReturn(new StaffApplicationDetail(
                7, 12, 1L, "Applicant", "applicant@example.com", null, null, List.of(),
                StaffApplicationStatus.SUBMITTED, LocalDateTime.now(), List.of(), List.of()));

        mockMvc.perform(multipart("/api/v1/staff-recruitments/12/applications")
                        .file(new MockMultipartFile("request", "blob", "application/json",
                                "{\"department_choices\":[{\"department_id\":31,\"priority\":1}],"
                                        .concat("\"answers\":[{\"question_id\":41,\"answer\":\"\"}]}").getBytes()))
                        .file(new MockMultipartFile("file_41", "portfolio.pdf", "application/pdf", new byte[]{1})))
                .andExpect(status().isCreated());
        verify(service).submit(eq(12), eq(1L), any(), argThat(files -> files.containsKey(41)));
    }

    @Test
    void JSON_요청_파트를_파일로_오인하지_않고_문항_파일만_전달한다() {
        MockMultipartHttpServletRequest multipart = new MockMultipartHttpServletRequest();
        multipart.addFile(new MockMultipartFile("request", "blob", "application/json", "{}".getBytes()));
        MockMultipartFile file = new MockMultipartFile("file_41", "portfolio.pdf", "application/pdf", new byte[]{1});
        multipart.addFile(file);
        when(service.submit(eq(12), eq(1L), any(), any())).thenReturn(new StaffApplicationDetail(
                7, 12, 1L, "Applicant", "applicant@example.com", null, null, List.of(),
                StaffApplicationStatus.SUBMITTED, LocalDateTime.now(), List.of(), List.of()));

        var response = controller.submitWithFiles(1L, 12, request, multipart);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        verify(service).submit(eq(12), eq(1L), any(), argThat(files ->
                files.size() == 1 && files.get(41) == file));
    }

    @Test
    void 예상하지_못한_파일_파트는_거부한다() {
        MockMultipartHttpServletRequest multipart = new MockMultipartHttpServletRequest();
        multipart.addFile(new MockMultipartFile("extra", "extra.pdf", "application/pdf", new byte[]{1}));

        assertThatThrownBy(() -> controller.submitWithFiles(1L, 12, request, multipart))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STAFF_APPLICATION_INVALID_ANSWER);
    }

    @Test
    void 본인_지원서_수정은_JSON_요청을_바인딩한다() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of()));
        var mapper = new ObjectMapper().findAndRegisterModules()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        var mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
        when(service.update(eq(12), eq(1L), any(), any(), any())).thenReturn(new StaffApplicationDetail(
                7, 12, 1L, "Applicant", "applicant@example.com", null, null, List.of(),
                StaffApplicationStatus.SUBMITTED, LocalDateTime.now(), List.of(), List.of()));

        mockMvc.perform(patch("/api/v1/staff-recruitments/12/applications/me")
                        .contentType("application/json")
                        .content("{\"department_choices\":[{\"department_id\":31,\"priority\":1}],"
                                + "\"answers\":[{\"question_id\":41,\"answer\":\"수정 답변\"}],"
                                + "\"removed_file_question_ids\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.application_id").value(7));
        verify(service).update(eq(12), eq(1L), argThat(command ->
                command.answers().get(0).answer().equals("수정 답변")), eq(java.util.Map.of()), eq(java.util.Set.of()));
    }

    @Test
    void 본인_지원서_수정은_파일_교체와_제거_목록을_바인딩한다() {
        MockMultipartHttpServletRequest multipart = new MockMultipartHttpServletRequest();
        multipart.addFile(new MockMultipartFile("request", "blob", "application/json", "{}".getBytes()));
        MockMultipartFile replacement = new MockMultipartFile("file_41", "new.pdf", "application/pdf", new byte[]{1});
        multipart.addFile(replacement);
        var update = new StaffApplicationRequests.UpdateRequest(request.departmentChoices(), request.answers(), List.of(), List.of(42));
        when(service.update(eq(12), eq(1L), any(), any(), any())).thenReturn(new StaffApplicationDetail(
                7, 12, 1L, "Applicant", "applicant@example.com", null, null, List.of(),
                StaffApplicationStatus.SUBMITTED, LocalDateTime.now(), List.of(), List.of()));

        controller.updateMineWithFiles(1L, 12, update, multipart);

        verify(service).update(eq(12), eq(1L), any(), argThat(files -> files.get(41) == replacement),
                eq(java.util.Set.of(42)));
    }

    @Test
    void 취소는_인증된_본인_ID로_서비스를_호출한다() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of()));
        var mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        mockMvc.perform(delete("/api/v1/staff-recruitments/12/applications/me"))
                .andExpect(status().isOk());
        verify(service).cancel(12, 1L);
    }
}
