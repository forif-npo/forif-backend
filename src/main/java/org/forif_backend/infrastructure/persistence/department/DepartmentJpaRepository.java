package org.forif_backend.infrastructure.persistence.department;

import org.forif_backend.domain.department.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DepartmentJpaRepository extends JpaRepository<Department, Long> {
    java.util.Optional<Department> findByDepartmentName(String departmentName);

    @Query("""
            SELECT d FROM Department d
            JOIN FETCH d.college
            ORDER BY d.college.collegeName ASC, d.departmentName ASC
            """)
    List<Department> findAllWithCollege();
}
