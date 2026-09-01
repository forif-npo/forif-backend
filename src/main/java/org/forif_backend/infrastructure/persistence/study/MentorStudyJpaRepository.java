package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.MentorStudy;
import org.forif_backend.domain.study.MentorStudyId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface MentorStudyJpaRepository extends JpaRepository<MentorStudy, MentorStudyId> {
    List<MentorStudy> findByStudyId(Integer studyId);
    boolean existsByMentorIdAndStudyActYearAndStudyActSemester(Long mentorId, int actYear, int actSemester);
    void deleteByStudyId(Integer studyId);
}
