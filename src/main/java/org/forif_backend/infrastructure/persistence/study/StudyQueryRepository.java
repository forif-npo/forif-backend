package org.forif_backend.infrastructure.persistence.study;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import jakarta.persistence.EntityManager;

import org.forif_backend.domain.study.*;
import org.forif_backend.common.type.SortCriteria;
import org.forif_backend.domain.user.User;
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
    private final QUser mentorUser = new QUser("mentorUser");
    private final QStudy mentorSearchStudy = new QStudy("mentorSearchStudy");

    public StudyQueryRepository(EntityManager em) {
        queryFactory = new JPAQueryFactory(em);
    }

    public List<Study> searchStudies(StudySearchCond cond, Integer cursor, int size) {
        List<Integer> studyIds = queryFactory
                .select(study.id)
                .distinct()
                .from(study)
                .leftJoin(study.tags, studyTag)
                .where(study.studyStatus.in(StudyStatus.APPROVED, StudyStatus.STARTED),
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

        return findStudiesWithTags(studyIds);
    }

    /**
     * 컬렉션 fetch join에 페이지 제한을 함께 적용하면 조인된 태그 행이 잘릴 수 있다.
     * 페이지 대상 스터디를 먼저 확정한 뒤, 각 스터디의 전체 태그를 조회한다.
     */
    private List<Study> findStudiesWithTags(List<Integer> studyIds) {
        if (studyIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(study).distinct()
                .leftJoin(study.tags, studyTag).fetchJoin()
                .where(study.id.in(studyIds))
                .orderBy(study.id.desc())
                .fetch();
    }

    public long countStudiesForUser(StudySearchCond cond) {
        Long count = queryFactory
                .select(study.countDistinct())
                .from(study)
                .leftJoin(study.tags, studyTag)
                .where(study.studyStatus.in(StudyStatus.APPROVED, StudyStatus.STARTED),
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
                        study.studyStatus.in(StudyStatus.APPROVED, StudyStatus.STARTED),
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

    public Map<Long, String> findMentorStudyNamesByUserIds(
            List<Long> userIds,
            Integer year,
            Integer semester
    ) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Set<String>> studyNamesByMentorId = new java.util.LinkedHashMap<>();
        collectMentorStudyNames(
                study.primaryMentor.id,
                userIds,
                year,
                semester,
                studyNamesByMentorId
        );
        collectMentorStudyNames(
                study.secondaryMentor.id,
                userIds,
                year,
                semester,
                studyNamesByMentorId
        );

        return studyNamesByMentorId.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> String.join(", ", entry.getValue())
                ));
    }

    public List<User> searchMentors(Long cursor, int size, String search) {
        return queryFactory
                .selectFrom(mentorUser)
                .where(
                        mentorStudyExists(null, null),
                        mentorCursorLt(cursor),
                        mentorSearchKeyword(search, null, null)
                )
                .orderBy(mentorUser.id.desc())
                .limit(size + 1)
                .fetch();
    }

    public List<User> searchMentorsWithOffset(int page, int size, String search, List<SortCriteria> sorting) {
        return queryFactory
                .selectFrom(mentorUser)
                .where(mentorStudyExists(null, null), mentorSearchKeyword(search, null, null))
                .orderBy(mentorOrders(sorting))
                .offset((long) page * size)
                .limit(size)
                .fetch();
    }

    public List<User> searchMentorsWithOffset(int page, int size, String search) {
        return searchMentorsWithOffset(page, size, search, List.of());
    }

    public long countMentors(String search) {
        Long count = queryFactory
                .select(mentorUser.count())
                .from(mentorUser)
                .where(mentorStudyExists(null, null), mentorSearchKeyword(search, null, null))
                .fetchOne();
        return count != null ? count : 0L;
    }

    public List<User> searchMentorsByYearSemester(
            int year,
            int semester,
            Long cursor,
            int size,
            String search
    ) {
        return queryFactory
                .selectFrom(mentorUser)
                .where(
                        mentorStudyExists(year, semester),
                        mentorCursorLt(cursor),
                        mentorSearchKeyword(search, year, semester)
                )
                .orderBy(mentorUser.id.desc())
                .limit(size + 1)
                .fetch();
    }

    public List<User> searchMentorsByYearSemesterWithOffset(
            int year,
            int semester,
            int page,
            int size,
            String search,
            List<SortCriteria> sorting
    ) {
        return queryFactory
                .selectFrom(mentorUser)
                .where(mentorStudyExists(year, semester), mentorSearchKeyword(search, year, semester))
                .orderBy(mentorOrders(sorting))
                .offset((long) page * size)
                .limit(size)
                .fetch();
    }

    public List<User> searchMentorsByYearSemesterWithOffset(
            int year,
            int semester,
            int page,
            int size,
            String search
    ) {
        return searchMentorsByYearSemesterWithOffset(year, semester, page, size, search, List.of());
    }

    private OrderSpecifier<?>[] mentorOrders(List<SortCriteria> sorting) {
        List<OrderSpecifier<?>> orders = new java.util.ArrayList<>();

        for (SortCriteria criterion : sorting) {
            switch (criterion.field()) {
                case "userId" -> orders.add(order(criterion, mentorUser.id));
                case "department" -> orders.add(order(criterion, mentorUser.department));
                case "name" -> orders.add(order(criterion, mentorUser.userName));
                default -> throw new IllegalStateException("Unsupported mentor sort field: " + criterion.field());
            }
        }

        orders.add(mentorUser.id.desc());
        return orders.toArray(OrderSpecifier[]::new);
    }

    public long countMentorsByYearSemester(int year, int semester, String search) {
        Long count = queryFactory
                .select(mentorUser.count())
                .from(mentorUser)
                .where(mentorStudyExists(year, semester), mentorSearchKeyword(search, year, semester))
                .fetchOne();
        return count != null ? count : 0L;
    }

    private void collectMentorStudyNames(
            com.querydsl.core.types.dsl.NumberPath<Long> mentorId,
            List<Long> userIds,
            Integer year,
            Integer semester,
            Map<Long, Set<String>> studyNamesByMentorId
    ) {
        queryFactory
                .select(mentorId, study.studyName)
                .from(study)
                .where(
                        mentorId.in(userIds),
                        yearEq(year),
                        semesterEq(semester),
                        study.studyStatus.in(StudyStatus.APPROVED, StudyStatus.STARTED)
                )
                .orderBy(study.studyName.asc())
                .fetch()
                .forEach(tuple -> {
                    Long id = tuple.get(mentorId);
                    String name = tuple.get(study.studyName);
                    if (id != null && name != null) {
                        studyNamesByMentorId
                                .computeIfAbsent(id, ignored -> new java.util.LinkedHashSet<>())
                                .add(name);
                    }
                });
    }

    private BooleanExpression mentorStudyExists(Integer year, Integer semester) {
        return JPAExpressions
                .selectOne()
                .from(study)
                .where(
                        yearEq(year),
                        semesterEq(semester),
                        study.studyStatus.in(StudyStatus.APPROVED, StudyStatus.STARTED),
                        study.primaryMentor.id.eq(mentorUser.id)
                                .or(study.secondaryMentor.id.eq(mentorUser.id))
                )
                .exists();
    }

    private BooleanExpression mentorCursorLt(Long cursor) {
        return cursor == null ? null : mentorUser.id.lt(cursor);
    }

    private BooleanExpression mentorSearchKeyword(String search, Integer year, Integer semester) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return mentorUser.userName.containsIgnoreCase(search)
                .or(JPAExpressions
                        .selectOne()
                        .from(mentorSearchStudy)
                        .where(
                                year == null ? null : mentorSearchStudy.actYear.eq(year),
                                semester == null ? null : mentorSearchStudy.actSemester.eq(semester),
                                mentorSearchStudy.studyStatus.in(StudyStatus.APPROVED, StudyStatus.STARTED),
                                mentorSearchStudy.studyName.containsIgnoreCase(search),
                                mentorSearchStudy.primaryMentor.id.eq(mentorUser.id)
                                        .or(mentorSearchStudy.secondaryMentor.id.eq(mentorUser.id))
                        )
                        .exists());
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
                        study.studyStatus.in(
                                StudyStatus.PENDING,
                                StudyStatus.RE_APPLIED,
                                StudyStatus.REJECTED
                        )
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
        List<Integer> studyIds = queryFactory
                .select(study.id)
                .distinct()
                .from(study)
                .leftJoin(study.tags, studyTag)
                .where(study.studyStatus.in(StudyStatus.APPROVED, StudyStatus.STARTED),
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

        return findStudiesWithTags(studyIds);
    }

    public List<Study> searchAdminStudiesWithOffset(
            int page,
            int size,
            Integer year,
            Integer semester,
            String search,
            List<StudyStatus> studyStatuses,
            List<SortCriteria> sorting
    ) {
        List<Integer> studyIds = queryFactory
                .select(study.id)
                .from(study)
                .leftJoin(study.tags, studyTag)
                .leftJoin(studyUser).on(studyUser.study.id.eq(study.id))
                .where(
                        studyStatusesIn(studyStatuses),
                        yearEq(year),
                        semesterEq(semester),
                        searchKeywordEq(search)
                )
                .groupBy(
                        study.id,
                        study.recruitStatus,
                        study.studyName,
                        study.primaryMentorName,
                        study.secondaryMentorName,
                        study.difficulty,
                        study.weekDay,
                        study.studyStatus
                )
                .orderBy(adminStudyOrders(sorting))
                .offset((long) page * size)
                .limit(size)
                .fetch();

        if (studyIds.isEmpty()) {
            return List.of();
        }

        Map<Integer, Study> studiesById = queryFactory
                .selectFrom(study).distinct()
                .leftJoin(study.tags, studyTag).fetchJoin()
                .where(study.id.in(studyIds))
                .fetch()
                .stream()
                .collect(Collectors.toMap(Study::getId, item -> item));

        return studyIds.stream()
                .map(studiesById::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private OrderSpecifier<?>[] adminStudyOrders(List<SortCriteria> sorting) {
        List<OrderSpecifier<?>> orders = new java.util.ArrayList<>();

        for (SortCriteria criterion : sorting) {
            switch (criterion.field()) {
                case "recruit_status" -> addNullableOrder(
                        orders, criterion, recruitStatusOrder(), study.recruitStatus.isNull());
                case "study_name" -> orders.add(order(criterion, study.studyName));
                case "primary_mentor_name" -> {
                    orders.add(order(criterion, study.primaryMentorName));
                    orders.add(order(criterion, study.secondaryMentorName));
                }
                case "tags" -> orders.add(order(criterion, studyTag.name.min()));
                case "difficulty" -> addNullableOrder(
                        orders, criterion, difficultyOrder(), study.difficulty.isNull());
                case "week_day" -> addNullableOrder(
                        orders, criterion, weekDayOrder(), study.weekDay.isNull());
                // 스터디 안에서 멘티 ID는 유일하므로 복합 키 대신 멘티 ID를 센다.
                // 복합 키 COUNT(DISTINCT ...)는 DB 방언에 따라 행 값 표현식으로 변환되어
                // 정렬 결과가 일관되지 않을 수 있다.
                case "mentee_count" -> orders.add(order(criterion, studyUser.user.id.countDistinct()));
                case "study_status" -> addNullableOrder(
                        orders, criterion, studyStatusOrder(), study.studyStatus.isNull());
                default -> throw new IllegalStateException("Unsupported study sort field: " + criterion.field());
            }
        }

        // 페이지 사이에서 동률 행이 흔들리지 않도록 기본 식별자 정렬을 끝에 고정한다.
        orders.add(study.id.desc());
        return orders.toArray(OrderSpecifier[]::new);
    }

    private <T extends Comparable<?>> OrderSpecifier<T> order(
            SortCriteria criterion,
            com.querydsl.core.types.Expression<T> expression
    ) {
        return new OrderSpecifier<>(
                criterion.direction().toOrder(),
                expression,
                OrderSpecifier.NullHandling.NullsLast
        );
    }

    private <T extends Comparable<?>> void addNullableOrder(
            List<OrderSpecifier<?>> orders,
            SortCriteria criterion,
            com.querydsl.core.types.Expression<T> expression,
            BooleanExpression isNull
    ) {
        orders.add(new CaseBuilder().when(isNull).then(1).otherwise(0).asc());
        orders.add(order(criterion, expression));
    }

    private NumberExpression<Integer> recruitStatusOrder() {
        return new CaseBuilder()
                .when(study.recruitStatus.eq(RecruitStatus.APPLICABLE)).then(1)
                .when(study.recruitStatus.eq(RecruitStatus.CLOSED)).then(2)
                .otherwise(99);
    }

    private NumberExpression<Integer> difficultyOrder() {
        return new CaseBuilder()
                .when(study.difficulty.eq(StudyDifficulty.EASY)).then(1)
                .when(study.difficulty.eq(StudyDifficulty.SEMI_EASY)).then(2)
                .when(study.difficulty.eq(StudyDifficulty.NORMAL)).then(3)
                .when(study.difficulty.eq(StudyDifficulty.SEMI_HARD)).then(4)
                .when(study.difficulty.eq(StudyDifficulty.HARD)).then(5)
                .otherwise(99);
    }

    private NumberExpression<Integer> weekDayOrder() {
        return new CaseBuilder()
                .when(study.weekDay.eq(1)).then(1)
                .when(study.weekDay.eq(2)).then(2)
                .when(study.weekDay.eq(3)).then(3)
                .when(study.weekDay.eq(4)).then(4)
                .when(study.weekDay.eq(5)).then(5)
                .when(study.weekDay.eq(6)).then(6)
                .when(study.weekDay.eq(0)).then(7)
                .otherwise(99);
    }

    private NumberExpression<Integer> studyStatusOrder() {
        return new CaseBuilder()
                .when(study.studyStatus.eq(StudyStatus.PENDING)).then(1)
                .when(study.studyStatus.eq(StudyStatus.RE_APPLIED)).then(2)
                .when(study.studyStatus.eq(StudyStatus.REJECTED)).then(3)
                .when(study.studyStatus.eq(StudyStatus.APPROVED)).then(4)
                .when(study.studyStatus.eq(StudyStatus.STARTED)).then(5)
                .otherwise(99);
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
                        // 주멘토 혹은 부멘토가 나이면서 + 실제 개설된 스터디만
                        study.primaryMentor.id.eq(mentorId)
                                .or(secondaryMentor.id.eq(mentorId)),
                        study.studyStatus.eq(StudyStatus.STARTED)
                )
                .orderBy(study.createdAt.desc())
                .fetch();
    }

    public List<Study> findStudyApplicationsByMentorId(Long mentorId, int actYear, int actSemester) {
        return queryFactory
                .selectFrom(study).distinct()
                .leftJoin(study.tags, studyTag).fetchJoin()
                .leftJoin(study.secondaryMentor, secondaryMentor)
                .where(
                        study.primaryMentor.id.eq(mentorId)
                                .or(secondaryMentor.id.eq(mentorId)),
                        study.autonomousFlag.isNull().or(study.autonomousFlag.isFalse()),
                        study.studyStatus.in(
                                        StudyStatus.PENDING,
                                        StudyStatus.RE_APPLIED,
                                        StudyStatus.REJECTED
                                )
                                .or(study.studyStatus.eq(StudyStatus.APPROVED)
                                        .and(study.actYear.eq(actYear))
                                        .and(study.actSemester.eq(actSemester)))
                )
                .orderBy(study.createdAt.desc())
                .fetch();
    }
}
