package org.forif_backend.infrastructure.persistence.user;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserRepository;
import org.springframework.stereotype.Repository;

import static org.forif_backend.domain.user.QUserApply.userApply;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final JPAQueryFactory queryFactory;
    private final UserApplyJpaRepository userApplyJpaRepository;


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


}
