package org.forif_backend.domain.dues;

import java.util.List;
import java.util.Optional;

public interface MemberSemesterCheckRepository {

    Optional<MemberSemesterCheck> findByUserIdAndYearSemester(Long userId, int year, int semester);

    List<MemberSemesterCheck> findAllByYearSemesterAndUserIds(int year, int semester, List<Long> userIds);

    MemberSemesterCheck save(MemberSemesterCheck memberSemesterCheck);

    /** 심사 중 합격을 번복했을 때만 확인 기록을 제거한다. 부원 명단 삭제 이력은 보존한다. */
    void deleteByUserIdAndYearSemester(Long userId, int year, int semester);
}
