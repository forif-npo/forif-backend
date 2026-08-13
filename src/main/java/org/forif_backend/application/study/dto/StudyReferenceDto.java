package org.forif_backend.application.study.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.study.ReferenceType;
import org.forif_backend.domain.study.StudyReference;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
@Builder
public class StudyReferenceDto {
    private final UUID id;
    private final ReferenceType referenceType;
    private final String content;

    public static StudyReferenceDto from(StudyReference reference) {
        return from(reference, reference.getContent());
    }

    public static StudyReferenceDto from(StudyReference reference, String content) {
        return StudyReferenceDto.builder()
                .id(reference.getId())
                .referenceType(reference.getReferenceType())
                .content(content)
                .build();
    }
}
