package org.forif_backend.infrastructure.persistence.semester;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.semester.ActiveSemester;
import org.forif_backend.domain.semester.SemesterChangeLog;
import org.forif_backend.domain.semester.SemesterRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SemesterRepositoryImpl implements SemesterRepository {

    private final ActiveSemesterJpaRepository activeSemesterJpaRepository;
    private final SemesterChangeLogJpaRepository semesterChangeLogJpaRepository;

    @Override
    public Optional<ActiveSemester> findActive() {
        return activeSemesterJpaRepository.findById(ActiveSemester.SINGLETON_ID);
    }

    @Override
    public Optional<ActiveSemester> findActiveForUpdate() {
        return activeSemesterJpaRepository.findByIdForUpdate(ActiveSemester.SINGLETON_ID);
    }

    @Override
    public ActiveSemester save(ActiveSemester activeSemester) {
        return activeSemesterJpaRepository.save(activeSemester);
    }

    @Override
    public void saveChangeLog(SemesterChangeLog log) {
        semesterChangeLogJpaRepository.save(log);
    }
}
