package org.forif_backend.infrastructure.persistence.study;

import jakarta.persistence.EntityManager;
import org.forif_backend.domain.study.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Disabled
public class StudyRepositoryImplTest {

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("전체 스터디를 조회한다")
    @Sql("/sql/user-test-data.sql")
    @Sql("/sql/study-test-data.sql")
    void getStudies_withNoCondition_returnsAllStudies() {
        // given
        StudySearchCond cond = StudySearchCond.builder().build();

        // when
        List<Study> result = studyRepository.getStudies(cond, null, 20);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result).isSortedAccordingTo(
                Comparator.comparing(Study::getId).reversed()
        );
    }

    @Test
    @DisplayName("개설 년도와 학기로 스터디를 필터링한다")
    @Sql("/sql/user-test-data.sql")
    @Sql("/sql/study-test-data.sql")
    void getStudies_withYearAndSemester_returnsFilteredStudies() {
        // given
        StudySearchCond cond = StudySearchCond.builder()
                .year(2025)
                .semester(2)
                .build();

        // when
        List<Study> result = studyRepository.getStudies(cond, null, 20);

        // then
        assertThat(result).allMatch(study ->
                study.getActYear() == 2025 && study.getActSemester() == 2
        );
    }

    @Test
    @DisplayName("난이도로 스터디를 필터링한다")
    @Sql("/sql/user-test-data.sql")
    @Sql("/sql/study-test-data.sql")
    void getStudies_withDifficulties_returnsFilteredStudies() {
        // given
        List<StudyDifficulty> targetDifficulties = Arrays.asList(
                StudyDifficulty.EASY,
                StudyDifficulty.SEMI_EASY,
                StudyDifficulty.NORMAL
        );

        StudySearchCond cond = StudySearchCond.builder()
                .difficulties(targetDifficulties)
                .build();

        // when
        List<Study> result = studyRepository.getStudies(cond, null, 20);

        // then
        assertThat(result).allMatch(study ->
                targetDifficulties.contains(study.getDifficulty())
        );
    }

    @Test
    @DisplayName("스터디 이름 또는 멘토 이름으로 키워드 검색을 수행한다")
    @Sql("/sql/study-test-data.sql")
    void getStudies_withSearchKeyword_returnsMatchingStudies() {
        // given
        StudySearchCond cond = StudySearchCond.builder()
                .searchKeyword("개발")
                .build();

        // when
        List<Study> result = studyRepository.getStudies(cond, null, 10);

        // then
        assertThat(result).allMatch(study -> study.getStudyName().contains("개발"));
    }

    @Test
    @DisplayName("모집 상태로 스터디를 필터링한다")
    @Sql("/sql/user-test-data.sql")
    @Sql("/sql/study-test-data.sql")
    void getStudies_withRecruitStatus_returnsFilteredStudies() {
        // given
        StudySearchCond cond = StudySearchCond.builder()
                .recruitStatus(RecruitStatus.APPLICABLE)
                .build();

        // when
        List<Study> result = studyRepository.getStudies(cond, null, 20);

        // then
        assertThat(result).allMatch(study -> study.getRecruitStatus() == RecruitStatus.APPLICABLE);
    }

    @Test
    @DisplayName("태그 이름으로 스터디를 필터링한다")
    @Sql("/sql/user-test-data.sql")
    @Sql("/sql/study-test-data.sql")
    void getStudies_withStudyTagNames_returnsFilteredStudies() {
        // given
        StudySearchCond cond = StudySearchCond.builder()
                .studyTagNames(Arrays.asList("backend", "frontend"))
                .build();
        
        // when
        List<Study> result = studyRepository.getStudies(cond, null, 20);
        
        // then
        assertThat(result).allSatisfy(study ->
            assertThat(study.getTags())
                .extracting(StudyTag::getName)
                .anySatisfy(name -> assertThat(name).isIn("backend", "frontend"))
        );
    }

    @Test
    @DisplayName("복합 조건으로 스터디를 조회한다")
    @Sql("/sql/user-test-data.sql")
    @Sql("/sql/study-test-data.sql")
    void getStudies_withMultipleConditions_returnsFilteredStudies() {
        // given
        StudySearchCond cond = StudySearchCond.builder()
                .year(2022)
                .semester(1)
                .difficulties(Arrays.asList(StudyDifficulty.EASY, StudyDifficulty.SEMI_HARD))
                .searchKeyword("Java")
                .build();

        // when
        List<Study> result = studyRepository.getStudies(cond, null, 20);

        // then
        assertThat(result).allMatch(study -> study.getActYear() == 2022 &&
                study.getActSemester() == 1 &&
                Arrays.asList(StudyDifficulty.EASY, StudyDifficulty.SEMI_HARD).contains(study.getDifficulty()) &&
                study.getStudyName().contains("Java"));
    }

    @Test
    @DisplayName("커서 기반 페이지네이션으로 페이징 처리한다")
    @Sql("/sql/user-test-data.sql")
    @Sql("/sql/study-test-data.sql")
    void getStudies_withCursorPaging_returnsPagedResults() {
        // given
        StudySearchCond cond = StudySearchCond.builder().build();

        // when - 첫 페이지 조회 (cursor=null)
        List<Study> firstPage = studyRepository.getStudies(cond, null, 10);

        // 다음 페이지 조회 (마지막 항목의 ID를 커서로 사용)
        Integer nextCursor = firstPage.get(firstPage.size() - 1).getId();
        List<Study> secondPage = studyRepository.getStudies(cond, nextCursor, 10);

        // then
        assertThat(firstPage).isNotEmpty();
        assertThat(secondPage).isNotEmpty();
        // 두 번째 페이지의 모든 ID는 커서보다 작아야 함
        assertThat(secondPage).allMatch(study -> study.getId() < nextCursor);
    }
}
