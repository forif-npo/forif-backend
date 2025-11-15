package org.forif_backend.domain.user;

import org.forif_backend.common.type.SortDirection;
import org.forif_backend.domain.study.Study;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findUserById(Long id);
    void createUserApply(UserApply userApply);
    boolean existUserApply(int year, int semester, User applier);
    List<UserApply> findUserApply(Integer studyId, Long page, Long pageSize, UserApplyStatus statusFilter, SortDirection applyDateDirection);
}
