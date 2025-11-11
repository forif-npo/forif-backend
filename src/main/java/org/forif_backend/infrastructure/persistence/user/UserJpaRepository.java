package org.forif_backend.infrastructure.persistence.user;

import org.forif_backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, Long> {

}
