package org.forif_backend.infrastructure.persistence.user;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.forif_backend.common.type.SortDirection;
import org.forif_backend.common.type.SortCriteria;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyStatus;
import org.forif_backend.domain.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

import static org.forif_backend.domain.study.QStudy.study;
import static org.forif_backend.domain.study.QStudyUser.studyUser;
import static org.forif_backend.domain.dues.QMemberSemesterCheck.memberSemesterCheck;
import static org.forif_backend.domain.user.QUser.user;
import static org.forif_backend.domain.user.QUserApply.userApply;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final JPAQueryFactory queryFactory;
    private final UserApplyJpaRepository userApplyJpaRepository;

    @Override
    public Optional<User> findUserById(Long id) {
        return userJpaRepository.findById(id);
    }

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return userJpaRepository.existsById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email);
    }

    @Override
    public void createUserApply(UserApply userApply) {
        userApplyJpaRepository.save(userApply);
    }

    @Override
    public void deleteUserApply(UserApply userApply) {
        userApplyJpaRepository.delete(userApply);
    }

    @Override
    public boolean existUserApply(int year, int semester, User applier) {
        Integer isExist = queryFactory.selectOne()
                .from(userApply)
                .where(
                        userApply.applyYear.eq(year),
                        userApply.applySemester.eq(semester),
                        userApply.applier.eq(applier)
                )
                .fetchFirst();
        return isExist != null;
    }

    @Override
    public Optional<UserApply> findUserApplyByYearAndSemesterAndUser(int year, int semester, User user) {
        UserApply result = queryFactory.selectFrom(userApply)
                .where(
                        userApply.applyYear.eq(year),
                        userApply.applySemester.eq(semester),
                        userApply.applier.eq(user)
                )
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    // 미사용
    @Override
    public void deleteById(Long id) {
        userJpaRepository.deleteById(id);
    }

    @Override
    public Page<UserApply> findUserApply(Integer studyId, Pageable pageable, UserApplyStatus statusFilter, SortDirection applyDateDirection) {
        List<UserApply> content = queryFactory.selectFrom(userApply)
                .leftJoin(userApply.applier).fetchJoin()
                .where(filterByStudyAndStatus(studyId, statusFilter))
                .orderBy(getSortOrder(applyDateDirection))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(userApply.count())
                .from(userApply)
                .where(filterByStudyAndStatus(studyId, statusFilter));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Optional<UserApply> findUserApplyById(Long applyId) {
        return Optional.ofNullable(userApplyJpaRepository.findByid(applyId));
    }

    private BooleanExpression filterByStudyAndStatus(Integer studyId, UserApplyStatus statusFilter) {
        BooleanExpression primaryCondition = userApply.primaryStudy.eq(studyId);
        if (statusFilter != null) {
            primaryCondition = primaryCondition.and(userApply.primaryStatus.eq(statusFilter));
        }

        BooleanExpression secondaryCondition = userApply.secondaryStudy.eq(studyId);
        if (statusFilter != null) {
            secondaryCondition = secondaryCondition.and(userApply.secondaryStatus.eq(statusFilter));
        }

        return primaryCondition.or(secondaryCondition);
    }

    private OrderSpecifier<?> getSortOrder(SortDirection direction) {
        return new OrderSpecifier<>(direction.toOrder(), userApply.createdAt);
    }

    @Override
    public Optional<User> findByPhoneNum(String phoneNum) {
        return userJpaRepository.findByPhoneNum(phoneNum);
    }

    @Override
    public List<User> searchUsersWithCursor(Long cursor, int size, String search) {
        return queryFactory
                .selectFrom(user)
                .where(
                        userCursorLt(cursor),
                        userSearchKeyword(search)
                )
                .orderBy(user.id.desc())
                .limit(size + 1)
                .fetch();
    }

    @Override
    public List<User> searchUsersWithOffset(int page, int size, String search, List<SortCriteria> sorting) {
        return queryFactory
                .selectFrom(user)
                .where(userSearchKeyword(search))
                .orderBy(userOrders(sorting))
                .offset((long) page * size)
                .limit(size)
                .fetch();
    }

    @Override
    public long countUsers(String search) {
        Long count = queryFactory
                .select(user.count())
                .from(user)
                .where(userSearchKeyword(search))
                .fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public List<User> searchUsersByYearSemester(int year, int semester, Long cursor, int size, String search) {
        return queryFactory
                .selectFrom(user).distinct()
                .join(studyUser).on(studyUser.user.id.eq(user.id))
                .join(study).on(studyUser.study.id.eq(study.id))
                .where(
                        study.actYear.eq(year),
                        study.actSemester.eq(semester),
                        userCursorLt(cursor),
                        userSearchKeyword(search)
                )
                .orderBy(user.id.desc())
                .limit(size + 1)
                .fetch();
    }

    @Override
    public List<User> searchUsersByYearSemesterWithOffset(int year, int semester, int page, int size, String search, List<SortCriteria> sorting) {
        return queryFactory
                .selectFrom(user).distinct()
                .join(studyUser).on(studyUser.user.id.eq(user.id))
                .join(study).on(studyUser.study.id.eq(study.id))
                .where(
                        study.actYear.eq(year),
                        study.actSemester.eq(semester),
                        userSearchKeyword(search)
                )
                .orderBy(userOrders(sorting))
                .offset((long) page * size)
                .limit(size)
                .fetch();
    }

    private OrderSpecifier<?>[] userOrders(List<SortCriteria> sorting) {
        List<OrderSpecifier<?>> orders = new java.util.ArrayList<>();

        for (SortCriteria criterion : sorting) {
            switch (criterion.field()) {
                case "userId" -> orders.add(order(criterion, user.id));
                case "department" -> orders.add(order(criterion, user.department));
                case "userName" -> orders.add(order(criterion, user.userName));
                default -> throw new IllegalStateException("Unsupported member sort field: " + criterion.field());
            }
        }

        orders.add(user.id.desc());
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

    @Override
    public long countUsersByYearSemester(int year, int semester, String search) {
        Long count = queryFactory
                .select(user.countDistinct())
                .from(user)
                .join(studyUser).on(studyUser.user.id.eq(user.id))
                .join(study).on(studyUser.study.id.eq(study.id))
                .where(
                        study.actYear.eq(year),
                        study.actSemester.eq(semester),
                        userSearchKeyword(search)
                )
                .fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public List<User> searchNotificationUsersWithCursor(Long cursor, int size, String search) {
        return queryFactory
                .selectFrom(user)
                .where(
                        userCursorLt(cursor),
                        hasPhoneNumber(),
                        notificationRecipientSearchKeyword(search)
                )
                .orderBy(user.id.desc())
                .limit(size + 1)
                .fetch();
    }

    @Override
    public long countNotificationUsers(String search) {
        Long count = queryFactory
                .select(user.count())
                .from(user)
                .where(
                        hasPhoneNumber(),
                        notificationRecipientSearchKeyword(search)
                )
                .fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public List<User> searchNotificationUsersByYearSemester(int year, int semester, Long cursor, int size, String search) {
        return queryFactory
                .selectFrom(user).distinct()
                .join(studyUser).on(studyUser.user.id.eq(user.id))
                .join(study).on(studyUser.study.id.eq(study.id))
                .where(
                        study.actYear.eq(year),
                        study.actSemester.eq(semester),
                        userCursorLt(cursor),
                        hasPhoneNumber(),
                        notificationRecipientSearchKeyword(search)
                )
                .orderBy(user.id.desc())
                .limit(size + 1)
                .fetch();
    }

    @Override
    public long countNotificationUsersByYearSemester(int year, int semester, String search) {
        Long count = queryFactory
                .select(user.countDistinct())
                .from(user)
                .join(studyUser).on(studyUser.user.id.eq(user.id))
                .join(study).on(studyUser.study.id.eq(study.id))
                .where(
                        study.actYear.eq(year),
                        study.actSemester.eq(semester),
                        hasPhoneNumber(),
                        notificationRecipientSearchKeyword(search)
                )
                .fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public List<User> searchApplicantsByYearSemester(int year, int semester, Long cursor, int size, String search) {
        return queryFactory
                .selectFrom(user).distinct()
                .join(userApply).on(userApply.applier.id.eq(user.id))
                .where(
                        userApply.applyYear.eq(year),
                        userApply.applySemester.eq(semester),
                        userCursorLt(cursor),
                        hasPhoneNumber(),
                        notificationRecipientSearchKeyword(search)
                )
                .orderBy(user.id.desc())
                .limit(size + 1)
                .fetch();
    }

    @Override
    public long countApplicantsByYearSemester(int year, int semester, String search) {
        Long count = queryFactory
                .select(user.countDistinct())
                .from(user)
                .join(userApply).on(userApply.applier.id.eq(user.id))
                .where(
                        userApply.applyYear.eq(year),
                        userApply.applySemester.eq(semester),
                        hasPhoneNumber(),
                        notificationRecipientSearchKeyword(search)
                )
                .fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public List<User> searchAcceptedApplicantsByYearSemester(
            int year, int semester, Long cursor, int size, String search
    ) {
        return searchApplicantsByDecision(
                year, semester, cursor, size, search, hasAcceptedStudyApplication());
    }

    @Override
    public long countAcceptedApplicantsByYearSemester(int year, int semester, String search) {
        return countApplicantsByDecision(year, semester, search, hasAcceptedStudyApplication());
    }

    @Override
    public List<User> searchRejectedApplicantsByYearSemester(
            int year, int semester, Long cursor, int size, String search
    ) {
        return searchApplicantsByDecision(
                year, semester, cursor, size, search,
                hasRejectedStudyApplication().and(hasNoAcceptedHistory(year, semester)));
    }

    @Override
    public long countRejectedApplicantsByYearSemester(int year, int semester, String search) {
        return countApplicantsByDecision(
                year, semester, search,
                hasRejectedStudyApplication().and(hasNoAcceptedHistory(year, semester)));
    }

    @Override
    public List<User> searchAcceptedUsersMissingDuesByYearSemester(
            int year, int semester, Long cursor, int size, String search
    ) {
        return searchAcceptedUsersWithIncompleteCheck(
                year, semester, cursor, size, search,
                memberSemesterCheck.id.isNull().or(memberSemesterCheck.duesPaid.isFalse())
        );
    }

    @Override
    public long countAcceptedUsersMissingDuesByYearSemester(int year, int semester, String search) {
        return countAcceptedUsersWithIncompleteCheck(
                year, semester, search,
                memberSemesterCheck.id.isNull().or(memberSemesterCheck.duesPaid.isFalse())
        );
    }

    @Override
    public List<User> searchAcceptedUsersMissingGoogleFormByYearSemester(
            int year, int semester, Long cursor, int size, String search
    ) {
        return searchAcceptedUsersWithIncompleteCheck(
                year, semester, cursor, size, search,
                memberSemesterCheck.id.isNull().or(memberSemesterCheck.googleFormSubmitted.isFalse())
        );
    }

    @Override
    public long countAcceptedUsersMissingGoogleFormByYearSemester(int year, int semester, String search) {
        return countAcceptedUsersWithIncompleteCheck(
                year, semester, search,
                memberSemesterCheck.id.isNull().or(memberSemesterCheck.googleFormSubmitted.isFalse())
        );
    }

    private List<User> searchAcceptedUsersWithIncompleteCheck(
            int year,
            int semester,
            Long cursor,
            int size,
            String search,
            BooleanExpression incompleteCheck
    ) {
        return queryFactory
                .selectFrom(user).distinct()
                .join(userApply).on(userApply.applier.id.eq(user.id))
                .leftJoin(memberSemesterCheck).on(
                        memberSemesterCheck.user.id.eq(user.id),
                        memberSemesterCheck.actYear.eq(year),
                        memberSemesterCheck.actSemester.eq(semester)
                )
                .where(
                        userApply.applyYear.eq(year),
                        userApply.applySemester.eq(semester),
                        hasAcceptedStudyApplication(),
                        userCursorLt(cursor),
                        hasPhoneNumber(),
                        notificationRecipientSearchKeyword(search),
                        incompleteCheck
                )
                .orderBy(user.id.desc())
                .limit(size + 1)
                .fetch();
    }

    private long countAcceptedUsersWithIncompleteCheck(
            int year,
            int semester,
            String search,
            BooleanExpression incompleteCheck
    ) {
        Long count = queryFactory
                .select(user.countDistinct())
                .from(user)
                .join(userApply).on(userApply.applier.id.eq(user.id))
                .leftJoin(memberSemesterCheck).on(
                        memberSemesterCheck.user.id.eq(user.id),
                        memberSemesterCheck.actYear.eq(year),
                        memberSemesterCheck.actSemester.eq(semester)
                )
                .where(
                        userApply.applyYear.eq(year),
                        userApply.applySemester.eq(semester),
                        hasAcceptedStudyApplication(),
                        hasPhoneNumber(),
                        notificationRecipientSearchKeyword(search),
                        incompleteCheck
                )
                .fetchOne();
        return count != null ? count : 0L;
    }

    private List<User> searchApplicantsByDecision(
            int year,
            int semester,
            Long cursor,
            int size,
            String search,
            BooleanExpression decision
    ) {
        return queryFactory
                .selectFrom(user).distinct()
                .join(userApply).on(userApply.applier.id.eq(user.id))
                .where(
                        userApply.applyYear.eq(year),
                        userApply.applySemester.eq(semester),
                        decision,
                        userCursorLt(cursor),
                        hasPhoneNumber(),
                        notificationRecipientSearchKeyword(search)
                )
                .orderBy(user.id.desc())
                .limit(size + 1)
                .fetch();
    }

    private long countApplicantsByDecision(
            int year,
            int semester,
            String search,
            BooleanExpression decision
    ) {
        Long count = queryFactory
                .select(user.countDistinct())
                .from(user)
                .join(userApply).on(userApply.applier.id.eq(user.id))
                .where(
                        userApply.applyYear.eq(year),
                        userApply.applySemester.eq(semester),
                        decision,
                        hasPhoneNumber(),
                        notificationRecipientSearchKeyword(search)
                )
                .fetchOne();
        return count != null ? count : 0L;
    }

    private BooleanExpression userCursorLt(Long cursor) {
        return cursor != null ? user.id.lt(cursor) : null;
    }

    private BooleanExpression userSearchKeyword(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return user.userName.containsIgnoreCase(search)
                .or(user.department.containsIgnoreCase(search));
    }

    private BooleanExpression hasPhoneNumber() {
        return user.phoneNum.isNotNull().and(user.phoneNum.ne(""));
    }

    private BooleanExpression hasAcceptedStudyApplication() {
        return userApply.primaryStatus.eq(UserApplyStatus.ACCEPT)
                .or(userApply.secondaryStatus.eq(UserApplyStatus.ACCEPT));
    }

    private BooleanExpression hasRejectedStudyApplication() {
        return userApply.primaryStatus.eq(UserApplyStatus.REJECT)
                .and(userApply.secondaryStudy.isNull()
                        .or(userApply.secondaryStatus.eq(UserApplyStatus.REJECT)));
    }

    /** 합격 시 생성된 확인 기록은 부원 삭제 뒤에도 남아 심사 불합격과 구분한다. */
    private BooleanExpression hasNoAcceptedHistory(int year, int semester) {
        return JPAExpressions.selectOne()
                .from(memberSemesterCheck)
                .where(
                        memberSemesterCheck.user.id.eq(user.id),
                        memberSemesterCheck.actYear.eq(year),
                        memberSemesterCheck.actSemester.eq(semester)
                )
                .notExists();
    }

    private BooleanExpression notificationRecipientSearchKeyword(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return user.userName.containsIgnoreCase(search)
                .or(user.id.stringValue().contains(search));
    }
}
