package org.forif_backend.domain.semester;

import java.util.Optional;

public interface SemesterRepository {

    Optional<ActiveSemester> findActive();

    /** 자율스터디처럼 학기 단위의 단일 자원을 생성할 때 동시 요청을 직렬화한다. */
    Optional<ActiveSemester> findActiveForUpdate();

    ActiveSemester save(ActiveSemester activeSemester);

    void saveChangeLog(SemesterChangeLog log);
}
