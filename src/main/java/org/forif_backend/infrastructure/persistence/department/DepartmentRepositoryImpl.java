package org.forif_backend.infrastructure.persistence.department;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.department.Department;
import org.forif_backend.domain.department.DepartmentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DepartmentRepositoryImpl implements DepartmentRepository {
    private final DepartmentJpaRepository departmentJpaRepository;

    @Override
    public Optional<Department> findById(Long id) {
        return departmentJpaRepository.findById(id);
    }

    @Override
    public Optional<Department> findByDepartmentName(String departmentName) {
        return departmentJpaRepository.findByDepartmentName(departmentName);
    }

    @Override
    public List<Department> findAll() {
        return departmentJpaRepository.findAllWithCollege();
    }
}
