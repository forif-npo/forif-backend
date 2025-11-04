package org.forif_backend.application.study;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.forif_backend.domain.study.*;

@Service
@RequiredArgsConstructor
public class StudyService {
    
    private final StudyRepository studyRepository;
    
    public List<Study> getStudies(Long page, Long pageSize, Integer year, Integer semester,
                                     List<StudyDifficulty> difficulties, List<String> tags,
                                     RecruitStatus recruitStatus, String search) {
        
        // Build search condition
        StudySearchCond searchCond = StudySearchCond.builder()
            .year(year)
            .semester(semester)
            .difficulties(difficulties)
            .studyTagNames(tags)
            .recruitStatus(recruitStatus)
            .searchKeyword(search)
            .build();
        
        // Get studies from repository
        List<Study> studies = studyRepository.getStudies(searchCond, page, pageSize);
        
        return studies;
    }
}
