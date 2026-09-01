package org.forif_backend.infrastructure.persistence.semester;

import org.forif_backend.domain.semester.ActiveSemester;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ActiveSemesterJpaRepository extends JpaRepository<ActiveSemester, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT semester FROM ActiveSemester semester WHERE semester.id = :id")
    Optional<ActiveSemester> findByIdForUpdate(@Param("id") Integer id);
}
