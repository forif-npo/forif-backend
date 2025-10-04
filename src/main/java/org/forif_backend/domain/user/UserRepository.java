package org.forif_backend.domain.user;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteById(Long id);

    // FW-1-0 feature
    Optional<User> findUserById(Long id);
    void createUserApply(UserApply userApply);
    boolean existUserApply(int year, int semester, User applier);
}
