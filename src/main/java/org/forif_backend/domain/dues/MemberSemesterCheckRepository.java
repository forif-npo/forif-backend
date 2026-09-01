package org.forif_backend.domain.dues;

import java.util.List;
import java.util.Optional;

public interface MemberSemesterCheckRepository {

    Optional<MemberSemesterCheck> findByUserIdAndYearSemester(Long userId, int year, int semester);

    List<MemberSemesterCheck> findAllByYearSemesterAndUserIds(int year, int semester, List<Long> userIds);

    MemberSemesterCheck save(MemberSemesterCheck memberSemesterCheck);
}
