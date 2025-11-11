package org.forif_backend.domain.user;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findUserById(Long id);
    void createUserApply(UserApply userApply);
    boolean existUserApply(int year, int semester, User applier);

    User save(User user);

    Optional<User> findById(Long id);

    boolean existsById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

}
