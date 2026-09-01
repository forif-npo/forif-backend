package org.forif_backend.infrastructure.persistence.hackathon;

import org.forif_backend.domain.hackathon.HackathonSubmissionTechStack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HackathonSubmissionTechStackJpaRepository extends JpaRepository<HackathonSubmissionTechStack, Long> {

    void deleteBySubmission_Id(Long submissionId);

    List<HackathonSubmissionTechStack> findBySubmission_IdInOrderByDisplayOrderAsc(List<Long> submissionIds);
}
