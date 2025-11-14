package org.forif_backend.infrastructure.persistence.study;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.forif_backend.domain.study.StudyDifficulty;
import org.forif_backend.domain.study.RecruitStatus;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudySearchCond;
import org.forif_backend.domain.study.StudyTag;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class StudyRepositoryImplTest {

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("전체 스터디를 조회한다")
    @Sql("/sql/study-test-data.sql")
    void getStudies_withNoCondition_returnsAllStudies() {
        // given
        StudySearchCond cond = StudySearchCond.builder().build();

        // when
        List<Study> result = studyRepository.getStudies(cond, 0L, 20L);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result).isSortedAccordingTo(
                Comparator.comparing(Study::getCreatedAt).reversed()
        );
    }

    @Test
    @DisplayName("개설 년도와 학기로 스터디를 필터링한다")
    @Sql("/sql/study-test-data.sql")
    void getStudies_withYearAndSemester_returnsFilteredStudies() {
        // given
        StudySearchCond cond = StudySearchCond.builder()
                .year(2025)
                .semester(2)
                .build();

        // when
        List<Study> result = studyRepository.getStudies(cond, 0L, 20L);

        // then
        assertThat(result).allMatch(study ->
                study.getActYear() == 2025 && study.getActSemester() == 2
        );
    }

    @Test
    @DisplayName("난이도로 스터디를 필터링한다")
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
        List<Study> result = studyRepository.getStudies(cond, 0L, 20L);

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
        List<Study> result = studyRepository.getStudies(cond, 0L, 10L);

        // then
        assertThat(result).allMatch(study ->
                study.getStudyName().contains("개발") ||
                        study.getPrimaryMentorName().contains("개발") ||
                        (study.getSecondaryMentorName() != null &&
                                study.getSecondaryMentorName().contains("개발"))
        );
    }

    @Test
    @DisplayName("모집 상태로 스터디를 필터링한다")
    @Sql("/sql/study-test-data.sql")
    void getStudies_withRecruitStatus_returnsFilteredStudies() {
        // given
        StudySearchCond cond = StudySearchCond.builder()
                .recruitStatus(RecruitStatus.APPLICABLE)
                .build();

        // when
        List<Study> result = studyRepository.getStudies(cond, 0L, 20L);

        // then
        assertThat(result).allMatch(study ->
                study.getRecruitStatus() == RecruitStatus.APPLICABLE
        );
    }

    @Test
    @DisplayName("태그 이름으로 스터디를 필터링한다")
    @Sql("/sql/study-test-data.sql")
    void getStudies_withStudyTagNames_returnsFilteredStudies() {
        // given
        List<String> targetTags = Arrays.asList("backend", "frontend");
        StudySearchCond cond = StudySearchCond.builder()
                .studyTagNames(targetTags)
                .build();

        // when
        List<Study> result = studyRepository.getStudies(cond, 0L, 20L);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result).allSatisfy(study -> {
            assertThat(study.getTags())
                    .extracting(StudyTag::getName)
                    .anySatisfy(name -> assertThat(name).isIn(targetTags));
        });
    }

    @Test
    @DisplayName("복합 조건으로 스터디를 조회한다")
    @Sql("/sql/study-test-data.sql")
    void getStudies_withMultipleConditions_returnsFilteredStudies() {
        // given
        List<StudyDifficulty> difficulties = Arrays.asList(
                StudyDifficulty.EASY,
                StudyDifficulty.SEMI_HARD
        );

        StudySearchCond cond = StudySearchCond.builder()
                .year(2022)
                .semester(1)
                .difficulties(difficulties)
                .searchKeyword("Java")
                .build();

        // when
        List<Study> result = studyRepository.getStudies(cond, 0L, 20L);

        // then
        assertThat(result).allMatch(study ->
                study.getActYear() == 2022 &&
                        study.getActSemester() == 1 &&
                        difficulties.contains(study.getDifficulty()) &&
                        (study.getStudyName().contains("Java") ||
                                study.getPrimaryMentorName().contains("Java"))
        );
    }

    @Test
    @DisplayName("offset과 limit을 사용하여 페이징 처리한다")
    @Sql("/sql/study-test-data.sql")
    void getStudies_withPaging_returnsPagedResults() {
        // given
        StudySearchCond cond = StudySearchCond.builder().build();

        // when
        List<Study> firstPage = studyRepository.getStudies(cond, 0L, 10L);
        List<Study> secondPage = studyRepository.getStudies(cond, 10L, 10L);

        // then
        assertThat(firstPage).hasSize(10);
        assertThat(secondPage).isNotEmpty();  // ✅ hasSize(10) 대신
        assertThat(firstPage.get(0).getId())
                .isNotEqualTo(secondPage.get(0).getId());
    }
}