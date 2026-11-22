package org.forif_backend.web.staffrecruitment.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class StaffInterviewScheduleRequestValidationTest {

    @Test
    void 시작_시각을_생략하면_배정과_회장단_가능_시간_모두_거부한다() throws Exception {
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var assignment = mapper.readValue("{\"date\":\"2026-12-01\"}",
                StaffInterviewScheduleRequests.SlotRequest.class);
        var hostSlots = mapper.readValue("{\"slots\":[{\"date\":\"2026-12-01\"}]}",
                StaffInterviewScheduleRequests.HostSlotsRequest.class);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(assignment)).anySatisfy(violation ->
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("startMinute"));
            assertThat(validator.validate(hostSlots)).anySatisfy(violation ->
                    assertThat(violation.getPropertyPath().toString()).contains("startMinute"));
            assertThat(validator.validate(new StaffInterviewScheduleRequests.SlotRequest(
                    LocalDate.of(2026, 12, 1), 0))).isEmpty();
        }
    }
}
