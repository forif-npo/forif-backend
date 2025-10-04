package org.forif_backend.web.study.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StudiesResponse {
    private List<StudyResponse> studies;
}
