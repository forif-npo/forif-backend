package org.forif_backend.infrastructure.persistence.study;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import com.querydsl.core.Tuple;
import jakarta.persistence.EntityManager;

import org.forif_backend.domain.study.*;
import org.forif_backend.domain.user.QUser;
import org.springframework.stereotype.Repository;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

@Repository
public class StudyQueryRepository {
    private final JPAQueryFactory queryFactory;
    private final QStudy study = QStudy.study;
    private final QStudyTag studyTag = QStudyTag.studyTag;
    private final QStudyUser studyUser = QStudyUser.studyUser;
    private final QMentorStudy mentorStudy = QMentorStudy.mentorStudy;
    private final QUser secondaryMentor = new QUser("secondaryMentor");

    public StudyQueryRepository(EntityManager em) {
        queryFactory = new JPAQueryFactory(em);
    }

    public List<Study> searchStudies(StudySearchCond cond, Integer cursor, int size) {
        return queryFactory
                .selectFrom(study).distinct()
                .leftJoin(study.tags, studyTag).fetchJoin()
                .where(study.studyStatus.eq(StudyStatus.APPROVED),
                        cursorLt(cursor),
                        yearEq(cond.getYear()),
                        semesterEq(cond.getSemester()),
                        difficultiesIn(cond.getDifficulties()),
                        recruitStatusEq(cond.getRecruitStatus()),
                        searchKeywordEq(cond.getSearchKeyword()),
                        tagsIn(cond.getStudyTagNames()))
                .orderBy(study.id.desc())
                .limit(size + 1)
                .fetch();
    }

    public long countStudiesForUser(StudySearchCond cond) {
        Long count = queryFactory
                .select(study.countDistinct())
                .from(study)
                .leftJoin(study.tags, studyTag)
                .where(study.studyStatus.eq(StudyStatus.APPROVED),
                        yearEq(cond.getYear()),
                        semesterEq(cond.getSemester()),
                        difficultiesIn(cond.getDifficulties()),
                        recruitStatusEq(cond.getRecruitStatus()),
                        searchKeywordEq(cond.getSearchKeyword()),
                        tagsIn(cond.getStudyTagNames()))
                .fetchOne();
        return count != null ? count : 0L;
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

    private BooleanExpression studyStatusesIn(List<StudyStatus> studyStatuses) {
        if (studyStatuses == null || studyStatuses.isEmpty()) {
            return null;
        }

        return study.studyStatus.in(studyStatuses);
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

    /** 해당 학기에 멘토(주·부)로 등록된 유저 ID를 한 번에 추린다 */
    public Set<Long> findMentorUserIdsByUserIds(List<Long> userIds, int year, int semester) {
        if (userIds == null || userIds.isEmpty()) {
            return Set.of();
        }

        List<Tuple> results = queryFactory
                .select(study.primaryMentor.id, study.secondaryMentor.id)
                .from(study)
                .where(
                        study.actYear.eq(year),
                        study.actSemester.eq(semester),
                        study.primaryMentor.id.in(userIds).or(study.secondaryMentor.id.in(userIds))
                )
                .fetch();

        Set<Long> mentorIds = new HashSet<>();
        for (Tuple t : results) {
            Long primary = t.get(study.primaryMentor.id);
            Long secondary = t.get(study.secondaryMentor.id);
            if (primary != null && userIds.contains(primary)) mentorIds.add(primary);
            if (secondary != null && userIds.contains(secondary)) mentorIds.add(secondary);
        }
        return mentorIds;
    }

    public Map<Long, String> findCurrentStudyNamesByUserIds(List<Long> userIds, int year, int semester) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        List<Tuple> results = queryFactory
                .select(studyUser.user.id, study.studyName)
                .from(studyUser)
                .join(studyUser.study, study)
                .where(
                        studyUser.user.id.in(userIds),
                        study.actYear.eq(year),
                        study.actSemester.eq(semester)
                )
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        t -> t.get(studyUser.user.id),
                        t -> t.get(study.studyName),
                        (existing, replacement) -> existing
                ));
    }

    public Map<Long, List<Study>> findCurrentStudiesByUserIds(List<Long> userIds, int year, int semester) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        List<Tuple> results = queryFactory
                .select(studyUser.user.id, study)
                .from(studyUser)
                .join(studyUser.study, study)
                .where(
                        studyUser.user.id.in(userIds),
                        study.actYear.eq(year),
                        study.actSemester.eq(semester)
                )
                .orderBy(study.studyName.asc())
                .fetch();

        return groupStudiesByUserId(results, studyUser.user.id);
    }

    public Map<Long, List<Study>> findCurrentMentorStudiesByUserIds(List<Long> userIds, int year, int semester) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        List<Tuple> results = queryFactory
                .select(mentorStudy.mentor.id, study)
                .from(mentorStudy)
                .join(mentorStudy.study, study)
                .where(
                        mentorStudy.mentor.id.in(userIds),
                        study.actYear.eq(year),
                        study.actSemester.eq(semester)
                )
                .orderBy(study.studyName.asc())
                .fetch();

        return groupStudiesByUserId(results, mentorStudy.mentor.id);
    }

    private Map<Long, List<Study>> groupStudiesByUserId(List<Tuple> results, com.querydsl.core.types.Expression<Long> userIdExpression) {
        return results.stream()
                .collect(Collectors.groupingBy(
                        t -> t.get(userIdExpression),
                        Collectors.mapping(t -> t.get(study), Collectors.toList())
                ));
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

    public List<Study> findAllStudiesByMentorId(Long mentorId) {
        return queryFactory
                .selectFrom(study).distinct()
                // 1. 태그는 목록 출력 시 성능을 위해 Fetch Join
                .leftJoin(study.tags, studyTag).fetchJoin()

                // 2. 부멘토는 null일 때 데이터 누락 방지를 위해 명시적 Left Join
                .leftJoin(study.secondaryMentor, secondaryMentor)

                .where(
                        study.primaryMentor.id.eq(mentorId) // FK 직접 비교 (효율적)
                                .or(secondaryMentor.id.eq(mentorId)), // 별칭으로 비교 (안전함)
                        study.studyStatus.ne(StudyStatus.APPROVED)
                )
                .orderBy(study.id.desc())
                .fetch();
    }

    public List<Study> searchStudiesWithCursor(Integer cursor, int size, Integer year, Integer semester, String search, List<StudyStatus> studyStatuses) {
        return queryFactory
                .selectFrom(study).distinct()
                .leftJoin(study.tags, studyTag).fetchJoin()
                .where(
                        studyStatusesIn(studyStatuses),
                        cursorLt(cursor),
                        yearEq(year),
                        semesterEq(semester),
                        searchKeywordEq(search)
                )
                .orderBy(study.id.desc())
                .limit(size + 1)
                .fetch();
    }

    public long countStudies(Integer year, Integer semester, String search, List<StudyStatus> studyStatuses) {
        Long count = queryFactory
                .select(study.count())
                .from(study)
                .where(
                        studyStatusesIn(studyStatuses),
                        yearEq(year),
                        semesterEq(semester),
                        searchKeywordEq(search)
                )
                .fetchOne();
        return count != null ? count : 0L;
    }

    public List<Study> searchStudiesWithOffset(StudySearchCond cond, int page, int size) {
        return queryFactory
                .selectFrom(study).distinct()
                .leftJoin(study.tags, studyTag).fetchJoin()
                .where(study.studyStatus.eq(StudyStatus.APPROVED),
                        yearEq(cond.getYear()),
                        semesterEq(cond.getSemester()),
                        difficultiesIn(cond.getDifficulties()),
                        recruitStatusEq(cond.getRecruitStatus()),
                        searchKeywordEq(cond.getSearchKeyword()),
                        tagsIn(cond.getStudyTagNames()))
                .orderBy(study.id.desc())
                .offset((long) page * size)
                .limit(size)
                .fetch();
    }

    public List<Study> searchAdminStudiesWithOffset(int page, int size, Integer year, Integer semester, String search, List<StudyStatus> studyStatuses) {
        return queryFactory
                .selectFrom(study).distinct()
                .leftJoin(study.tags, studyTag).fetchJoin()
                .where(
                        studyStatusesIn(studyStatuses),
                        yearEq(year),
                        semesterEq(semester),
                        searchKeywordEq(search)
                )
                .orderBy(study.id.desc())
                .offset((long) page * size)
                .limit(size)
                .fetch();
    }

    public Map<Integer, Long> countMenteesByStudyIds(List<Integer> studyIds) {
        List<Tuple> results = queryFactory
                .select(studyUser.study.id, studyUser.count())
                .from(studyUser)
                .where(studyUser.study.id.in(studyIds))
                .groupBy(studyUser.study.id)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(studyUser.study.id),
                        tuple -> tuple.get(studyUser.count())
                ));
    }

    private BooleanExpression cursorLt(Integer cursor) {
        if (cursor == null) {
            return null;
        }
        return study.id.lt(cursor);
    }

    public List<Study> findStudiesByMentorId(Long mentorId) {
        return queryFactory
                .selectFrom(study).distinct()
                .leftJoin(study.tags, studyTag).fetchJoin() // 태그 N+1 방어
                .leftJoin(study.secondaryMentor, secondaryMentor) // 부멘토 null 방어
                .where(
                        // 주멘토 혹은 부멘토가 나이면서 + 승인 완료된 스터디만
                        study.primaryMentor.id.eq(mentorId)
                                .or(secondaryMentor.id.eq(mentorId)),
                        study.studyStatus.eq(StudyStatus.APPROVED)
                )
                .orderBy(study.createdAt.desc())
                .fetch();
    }
}
