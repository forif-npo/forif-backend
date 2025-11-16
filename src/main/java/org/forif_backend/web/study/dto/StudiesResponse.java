package org.forif_backend.web.study.dto;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Getter;

import org.forif_backend.application.study.dto.StudyDto;

@Getter
@Builder
public class StudiesResponse {
    private List<StudyResponse> studies;

    public static StudiesResponse from(List<StudyDto> studies) {
        List<StudyResponse> studyResponses = studies.stream()
                .map(StudyResponse::from)
                .collect(Collectors.toList());

        StudiesResponse response = StudiesResponse.builder()
                .studies(studyResponses)
                .build();

        return response;
    }
}
