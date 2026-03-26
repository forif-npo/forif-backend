package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.StudyUser;
import org.forif_backend.domain.study.StudyUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudyUserJpaRepository extends JpaRepository<StudyUser, StudyUserId> {

    @Query("SELECT su FROM StudyUser su WHERE su.user.id = :userId AND su.study.id = :studyId")
    Optional<StudyUser> findByUserIdAndStudyId(@Param("userId") Long userId, @Param("studyId") Integer studyId);

    void deleteByStudyId(Integer studyId);

    void deleteByUserIdAndStudyId(Long userId, Integer studyId);
}
