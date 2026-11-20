package org.forif_backend.infrastructure.persistence.team;

import org.forif_backend.domain.team.ClubDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClubDepartmentJpaRepository extends JpaRepository<ClubDepartment, Long> {
    Optional<ClubDepartment> findByName(String name);
}
