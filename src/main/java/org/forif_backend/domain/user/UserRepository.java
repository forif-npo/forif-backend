package org.forif_backend.domain.user;

import org.forif_backend.common.type.SortDirection;
import org.forif_backend.domain.study.Study;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    boolean existsById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteById(Long id);

    Optional<User> findUserById(Long id);

    void createUserApply(UserApply userApply);

    boolean existUserApply(int year, int semester, User applier);

    Page<UserApply> findUserApply(Integer studyId, Pageable pageable, UserApplyStatus statusFilter, SortDirection applyDateDirection);

    UserApply findUserApplyById(Long applyId);
}
