package org.forif_backend.infrastructure.persistence.user;

import org.forif_backend.domain.user.UserApply;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserApplyJpaRepository extends JpaRepository<UserApply, Long> {
}
