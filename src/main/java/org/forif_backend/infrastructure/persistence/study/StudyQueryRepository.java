package org.forif_backend.infrastructure.persistence.study;

import java.util.List;
import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import org.forif_backend.domain.study.StudyDifficulty;
import org.forif_backend.domain.study.QStudy;
import org.forif_backend.domain.study.QStudyTag;
import org.forif_backend.domain.study.QStudyUser;
import org.forif_backend.domain.study.RecruitStatus;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudySearchCond;

@Repository
public class StudyQueryRepository {
    private final JPAQueryFactory queryFactory;
    private final QStudy study = QStudy.study;
    private final QStudyTag studyTag = QStudyTag.studyTag;
    private final QStudyUser studyUser = QStudyUser.studyUser;

    public StudyQueryRepository(EntityManager em) {
        queryFactory = new JPAQueryFactory(em);
    }

    public List<Study> searchStudies(StudySearchCond cond, Long offset, Long limit) {

        // distinct로 중복 제거
        List<Study> studies = queryFactory
                .selectFrom(study).distinct()
                .leftJoin(study.tags, studyTag).fetchJoin()
                .where(yearEq(cond.getYear()),
                        semesterEq(cond.getSemester()),
                        difficultiesIn(cond.getDifficulties()),
                        recruitStatusEq(cond.getRecruitStatus()),
                        searchKeywordEq(cond.getSearchKeyword()),
                        tagsIn(cond.getStudyTagNames()))
                .orderBy(study.createdAt.desc())
                .offset(offset)
                .limit(limit)
                .fetch();

        return studies;     
    }

    private BooleanExpression yearEq(Integer year) {
        if (year == null) {
            return null;
        }

        return study.actYear.eq(year);
    }

    private BooleanExpression semesterEq(Integer semester) {
        if (semester == null) {
            return null;
        }

        return study.actSemester.eq(semester);
    }

    private BooleanExpression difficultiesIn(List<StudyDifficulty> difficulties) {
        if (difficulties == null || difficulties.isEmpty()) {
            return null;
        }

        return study.difficulty.in(difficulties);
    }

    private BooleanExpression recruitStatusEq(RecruitStatus recruitStatus) {
        if (recruitStatus == null) {
            return null;
        }

        return study.recruitStatus.eq(recruitStatus);
    }

    private BooleanExpression searchKeywordEq(String searchKeyword) {
        if (searchKeyword == null) {
            return null;
        }

        return study.studyName.containsIgnoreCase(searchKeyword)
                .or(study.primaryMentorName.containsIgnoreCase(searchKeyword))
                .or(study.secondaryMentorName.containsIgnoreCase(searchKeyword));
    }
    
    private BooleanExpression tagsIn(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return null;
        }

        return studyTag.name.in(tagNames);
    }

    public List<Study> findStudiesByUserId(Long userId) {
        return queryFactory
                .selectFrom(study).distinct()
                .leftJoin(study.tags, studyTag).fetchJoin()
                .join(studyUser).on(studyUser.study.id.eq(study.id))
                .where(studyUser.user.id.eq(userId))
                .orderBy(study.actYear.desc(), study.actSemester.desc())
                .fetch();
    }

    public List<Study> findAllStudiesByMentorIdAndIsApplied(Long mentorId, Boolean isApplied) {
        return queryFactory
                .selectFrom(study).distinct()
                .leftJoin(study.tags, studyTag).fetchJoin()
                .where(
                        study.primaryMentor.id.eq(mentorId)
                                .or(study.secondaryMentor.id.eq(mentorId)),
                        isApplied != null ? study.isApplied.eq(isApplied) : null
                )
                .orderBy(study.createdAt.desc())
                .fetch();
    }
}
