package org.forif_backend.application.study.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.forif_backend.domain.study.StudyTag;

@Getter
@RequiredArgsConstructor
@Builder
public class StudyTagDto {
    private final Long id;
    private final String name;

    public static StudyTagDto from(StudyTag studyTagEntity) {
        return StudyTagDto.builder()
                .id(studyTagEntity.getId())
                .name(studyTagEntity.getName())
                .build();
    }
}
