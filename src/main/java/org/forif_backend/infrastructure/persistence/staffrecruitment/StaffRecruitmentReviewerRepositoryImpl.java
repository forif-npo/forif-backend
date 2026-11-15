package org.forif_backend.infrastructure.persistence.staffrecruitment;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentReviewer;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentReviewerRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StaffRecruitmentReviewerRepositoryImpl implements StaffRecruitmentReviewerRepository {

    private final StaffRecruitmentReviewerJpaRepository staffRecruitmentReviewerJpaRepository;

    @Override
    public StaffRecruitmentReviewer save(StaffRecruitmentReviewer reviewer) {
        return staffRecruitmentReviewerJpaRepository.save(reviewer);
    }

    @Override
    public void flush() {
        staffRecruitmentReviewerJpaRepository.flush();
    }

    @Override
    public Optional<StaffRecruitmentReviewer> findByRecruitmentIdAndUserId(Integer recruitmentId, Long userId) {
        return staffRecruitmentReviewerJpaRepository.findByRecruitmentIdAndUserId(recruitmentId, userId);
    }

    @Override
    public Optional<StaffRecruitmentReviewer> findByRecruitmentIdAndUserIdForUpdate(Integer recruitmentId,
                                                                                    Long userId) {
        return staffRecruitmentReviewerJpaRepository.findByRecruitmentIdAndUserIdForUpdate(recruitmentId, userId);
    }

    @Override
    public List<StaffRecruitmentReviewer> findAllByRecruitmentId(Integer recruitmentId) {
        return staffRecruitmentReviewerJpaRepository.findAllByRecruitmentId(recruitmentId);
    }

    @Override
    public List<Integer> findActiveRecruitmentIdsByUserId(Long userId) {
        return staffRecruitmentReviewerJpaRepository.findActiveRecruitmentIdsByUserId(userId, LocalDateTime.now());
    }
}
