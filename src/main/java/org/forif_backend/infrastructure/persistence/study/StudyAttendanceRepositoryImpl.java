package org.forif_backend.infrastructure.persistence.study;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.study.StudyAttendance;
import org.forif_backend.domain.study.StudyAttendanceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class StudyAttendanceRepositoryImpl implements StudyAttendanceRepository {

    private final StudyAttendanceJpaRepository studyAttendanceJpaRepository;

    @Override
    public List<StudyAttendance> findAllByStudyId(Integer studyId) {
        return studyAttendanceJpaRepository.findAllByStudyId(studyId);
    }

    @Override
    public void save(StudyAttendance studyAttendance) {
        studyAttendanceJpaRepository.save(studyAttendance);
    }

    @Override
    public void saveAll(List<StudyAttendance> studyAttendances) {
        studyAttendanceJpaRepository.saveAll(studyAttendances);
    }

    @Override
    public Map<Long, Long> countPresentByStudyId(Integer studyId) {
        return studyAttendanceJpaRepository.countPresentByStudyId(studyId).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
