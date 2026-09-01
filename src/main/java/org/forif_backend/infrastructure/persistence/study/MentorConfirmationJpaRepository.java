package org.forif_backend.infrastructure.persistence.study;

import java.util.List;
import java.util.Optional;
import org.forif_backend.domain.study.MentorConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MentorConfirmationJpaRepository extends JpaRepository<MentorConfirmation, Long> {

    List<MentorConfirmation> findAllByStudyId(Integer studyId);

    Optional<MentorConfirmation> findByStudyIdAndMentorId(Integer studyId, Long mentorId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO tb_mentor_confirmation
                (study_id, mentor_id, confirmation_object_key, created_at, updated_at)
            VALUES (:studyId, :mentorId, :confirmationObjectKey, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE
                confirmation_object_key = VALUES(confirmation_object_key),
                updated_at = VALUES(updated_at)
            """, nativeQuery = true)
    void upsert(
            @Param("studyId") Integer studyId,
            @Param("mentorId") Long mentorId,
            @Param("confirmationObjectKey") String confirmationObjectKey
    );

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM MentorConfirmation confirmation WHERE confirmation.study.id = :studyId")
    void deleteByStudyId(@Param("studyId") Integer studyId);
}
