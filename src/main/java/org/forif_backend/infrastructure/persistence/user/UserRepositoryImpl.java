package org.forif_backend.infrastructure.persistence.user;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.forif_backend.common.type.SortDirection;
import org.forif_backend.domain.study.Study;
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
    public UserApply findUserApplyById(Long applyId) {
        return userApplyJpaRepository.findByid(applyId);
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
}
