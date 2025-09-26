package org.forif_backend.domain.user;

public interface UserRepository {
    void createUserApply(UserApply userApply);
    boolean existUserApply(int year, int semester, User applier);
}
