package org.forif_backend.web.staffrecruitment.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StaffApplicationRequestValidationTest {

    @Test
    void 수정_목록의_null_원소를_거부한다() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var request = new StaffApplicationRequests.UpdateRequest(
                    Arrays.asList(new StaffApplicationRequests.ChoiceRequest(31, 1), null),
                    Arrays.asList(new StaffApplicationRequests.AnswerRequest(41, "답변"), null),
                    List.of(),
                    Arrays.asList(41, null));

            var paths = factory.getValidator().validate(request).stream()
                    .map(violation -> violation.getPropertyPath().toString()).toList();

            assertThat(paths).anyMatch(path -> path.startsWith("departmentChoices"));
            assertThat(paths).anyMatch(path -> path.startsWith("answers"));
            assertThat(paths).anyMatch(path -> path.startsWith("removedFileQuestionIds"));
        }
    }
}
