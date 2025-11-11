package org.forif_backend.domain.user;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findUserById(Long id);
    void createUserApply(UserApply userApply);
    boolean existUserApply(int year, int semester, User applier);
}
