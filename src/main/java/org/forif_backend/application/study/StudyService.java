package org.forif_backend.application.study;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.forif_backend.application.study.dto.StudyDto;
import org.forif_backend.application.study.dto.StudyInfo;
import org.forif_backend.application.study.dto.SemesterStudiesInfo;
import org.forif_backend.application.study.dto.UserStudiesResult;
import org.forif_backend.common.util.DateUtils;
import org.forif_backend.domain.study.*;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyService {
    
    private final StudyRepository studyRepository;
    private final MentorStudyRepository mentorStudyRepository;

    @Transactional(readOnly = true)
    public List<StudyDto> getStudies(Long page, Long pageSize, Integer year, Integer semester,
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

        return studies.stream().map(StudyDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<StudyDto> getMyCreatedStudies(Long mentorId) {

        return mentorStudyRepository.findStudiesWithTagsByMentorId(mentorId)
            .stream()
            .map(StudyDto::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public UserStudiesResult getUserStudies(Long userId) {
        // 1. userId로 스터디 목록 조회 (이미 연도, 학기 내림차순으로 정렬됨)
        List<Study> studies = studyRepository.findStudiesByUserId(userId);

        // 2. 현재 학기 정보
        int currentYear = DateUtils.getCurrentYear();
        int currentSemester = DateUtils.getCurrentSemester();

        // 3. SemesterStudiesInfo 리스트 생성 (한 학기에 스터디 1개만)
        List<SemesterStudiesInfo> semesters = studies.stream()
                .collect(Collectors.groupingBy(
                        study -> study.getActYear() + "-" + study.getActSemester(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                studyList -> {
                                    Study firstStudy = studyList.get(0);
                                    int year = firstStudy.getActYear();
                                    int semester = firstStudy.getActSemester();

                                    return SemesterStudiesInfo.builder()
                                            .year(year)
                                            .semester(semester)
                                            .semesterLabel(year + "-" + semester)
                                            .isCurrent(year == currentYear && semester == currentSemester)
                                            .study(StudyInfo.from(firstStudy))  // 첫 번째 스터디만 (한 학기에 1개)
                                            .build();
                                }
                        )
                ))
                .values()
                .stream()
                .sorted((s1, s2) -> {
                    // 연도 내림차순, 같으면 학기 내림차순
                    int yearCompare = s2.year().compareTo(s1.year());
                    if (yearCompare != 0) return yearCompare;
                    return s2.semester().compareTo(s1.semester());
                })
                .toList();

        // 6. UserStudiesResult 반환
        return UserStudiesResult.builder()
                .semesters(semesters)
                .build();
    }
}
