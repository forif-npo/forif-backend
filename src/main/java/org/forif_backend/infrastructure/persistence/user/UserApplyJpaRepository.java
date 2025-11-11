package org.forif_backend.infrastructure.persistence.user;

import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserApplyJpaRepository extends JpaRepository<UserApply, Long> {
    List<UserApply> findByApplier(User applier);
}
