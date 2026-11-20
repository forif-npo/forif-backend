package org.forif_backend.domain.team;

import java.util.Optional;

public interface ClubDepartmentRepository {
    Optional<ClubDepartment> findByName(String name);

    ClubDepartment save(ClubDepartment department);
}
