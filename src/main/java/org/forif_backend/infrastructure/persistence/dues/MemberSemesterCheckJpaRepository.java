package org.forif_backend.infrastructure.persistence.dues;

import org.forif_backend.domain.dues.MemberSemesterCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberSemesterCheckJpaRepository extends JpaRepository<MemberSemesterCheck, Long> {

    Optional<MemberSemesterCheck> findByUser_IdAndActYearAndActSemester(Long userId, int actYear, int actSemester);

    List<MemberSemesterCheck> findAllByActYearAndActSemesterAndUser_IdIn(
            int actYear,
            int actSemester,
            List<Long> userIds
    );
}
