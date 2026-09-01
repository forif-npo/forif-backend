package org.forif_backend.infrastructure.persistence.dues;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.dues.MemberSemesterCheck;
import org.forif_backend.domain.dues.MemberSemesterCheckRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberSemesterCheckRepositoryImpl implements MemberSemesterCheckRepository {

    private final MemberSemesterCheckJpaRepository memberSemesterCheckJpaRepository;

    @Override
    public Optional<MemberSemesterCheck> findByUserIdAndYearSemester(Long userId, int year, int semester) {
        return memberSemesterCheckJpaRepository.findByUser_IdAndActYearAndActSemester(userId, year, semester);
    }

    @Override
    public List<MemberSemesterCheck> findAllByYearSemesterAndUserIds(int year, int semester, List<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return memberSemesterCheckJpaRepository.findAllByActYearAndActSemesterAndUser_IdIn(year, semester, userIds);
    }

    @Override
    public MemberSemesterCheck save(MemberSemesterCheck memberSemesterCheck) {
        return memberSemesterCheckJpaRepository.save(memberSemesterCheck);
    }
}
