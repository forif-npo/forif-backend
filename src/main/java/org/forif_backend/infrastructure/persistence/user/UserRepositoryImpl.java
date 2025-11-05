package org.forif_backend.infrastructure.persistence.user;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static org.forif_backend.domain.user.QUserApply.userApply;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserApplyJpaRepository userApplyJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final JPAQueryFactory queryFactory;

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
}
