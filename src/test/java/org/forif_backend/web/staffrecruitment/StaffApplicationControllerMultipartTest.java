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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StaffApplicationControllerMultipartTest {

    private final StaffApplicationService service = mock(StaffApplicationService.class);
    private final StaffApplicationController controller = new StaffApplicationController(service);
    private final StaffApplicationRequests.SubmitRequest request = new StaffApplicationRequests.SubmitRequest(
            List.of(new StaffApplicationRequests.ChoiceRequest(31, 1)),
            List.of(new StaffApplicationRequests.AnswerRequest(41, "")));

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
                StaffApplicationStatus.SUBMITTED, LocalDateTime.now(), List.of()));

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
                StaffApplicationStatus.SUBMITTED, LocalDateTime.now(), List.of()));

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
}
