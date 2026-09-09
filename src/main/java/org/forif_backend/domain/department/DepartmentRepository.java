package org.forif_backend.domain.department;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository {
    Optional<Department> findById(Long id);
    Optional<Department> findByDepartmentName(String departmentName);
    List<Department> findAll();
}
