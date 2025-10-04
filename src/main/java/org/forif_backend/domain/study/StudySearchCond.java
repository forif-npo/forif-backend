package org.forif_backend.domain.study;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudySearchCond {
    private final Integer year;
    private final Integer semester;
    private final List<Integer> difficulties;
    private final List<String> studyTagNames;
    private final RecruitStatus recruitStatus;
    private final String searchKeyword;
}
