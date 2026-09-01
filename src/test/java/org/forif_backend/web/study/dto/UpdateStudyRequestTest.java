package org.forif_backend.web.study.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateStudyRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    void acceptsLegacyPatchFieldNames() throws Exception {
        UpdateStudyRequest request = objectMapper.readValue("""
                {
                  "title": "기존 계약 스터디명",
                  "study_location": "공학관",
                  "study_location_detail": "301호",
                  "study_tag_id": [1, 2],
                  "secondary_mentor_id": 20
                }
                """, UpdateStudyRequest.class);

        assertThat(request.getStudyName()).isEqualTo("기존 계약 스터디명");
        assertThat(request.getLocation()).isEqualTo("공학관");
        assertThat(request.getLocationDetail()).isEqualTo("301호");
        assertThat(request.getStudyTagIds()).isEqualTo(List.of(1L, 2L));
        assertThat(request.getSecondaryMentorId()).isEqualTo(20L);
        assertThat(request.isSecondaryMentorIdPresent()).isTrue();
    }

    @Test
    void distinguishesExplicitSecondaryMentorRemovalFromAnOmittedField() throws Exception {
        UpdateStudyRequest request = objectMapper.readValue(
                "{\"secondary_mentor_id\": null}", UpdateStudyRequest.class);

        assertThat(request.isSecondaryMentorIdPresent()).isTrue();
        assertThat(request.getSecondaryMentorId()).isNull();
    }
}
