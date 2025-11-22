package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.MentorStudy;
import org.forif_backend.domain.study.MentorStudyId;
import org.springframework.data.jpa.repository.JpaRepository;

interface MentorStudyJpaRepository extends JpaRepository<MentorStudy, MentorStudyId> {

}