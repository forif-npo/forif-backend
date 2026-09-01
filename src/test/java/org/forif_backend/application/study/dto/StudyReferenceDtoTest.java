package org.forif_backend.application.study.dto;

import org.forif_backend.domain.study.ReferenceType;
import org.forif_backend.domain.study.StudyReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudyReferenceDtoTest {

    @Test
    void exposesTheOriginalFileNameWithoutTheStorageUuidPrefix() {
        StudyReference reference = StudyReference.create(
                null,
                ReferenceType.FILE,
                "studies/references/123e4567-e89b-12d3-a456-426614174000-study-guide.pdf"
        );

        StudyReferenceDto dto = StudyReferenceDto.from(reference);

        assertThat(dto.getFileName()).isEqualTo("study-guide.pdf");
    }

    @Test
    void doesNotExposeAFileNameForUrlReferences() {
        StudyReference reference = StudyReference.create(
                null,
                ReferenceType.URL,
                "https://example.com/reference"
        );

        StudyReferenceDto dto = StudyReferenceDto.from(reference);

        assertThat(dto.getFileName()).isNull();
    }
}
