package org.forif_backend.infrastructure.persistence.team;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.team.ClubDepartment;
import org.forif_backend.domain.team.ClubDepartmentRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ClubDepartmentRepositoryImpl implements ClubDepartmentRepository {

    private final ClubDepartmentJpaRepository jpaRepository;

    @Override
    public Optional<ClubDepartment> findByName(String name) {
        return jpaRepository.findByName(name);
    }

    @Override
    public ClubDepartment save(ClubDepartment department) {
        return jpaRepository.save(department);
    }
}
