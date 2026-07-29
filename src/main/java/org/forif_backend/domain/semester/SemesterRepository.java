package org.forif_backend.domain.semester;

import java.util.Optional;

public interface SemesterRepository {

    Optional<ActiveSemester> findActive();

    ActiveSemester save(ActiveSemester activeSemester);

    void saveChangeLog(SemesterChangeLog log);
}
