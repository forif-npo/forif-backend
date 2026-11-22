package org.forif_backend.infrastructure.persistence.staffrecruitment;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.staffrecruitment.StaffInterviewAssignment;
import org.forif_backend.domain.staffrecruitment.StaffInterviewHostSlot;
import org.forif_backend.domain.staffrecruitment.StaffInterviewScheduleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StaffInterviewScheduleRepositoryImpl implements StaffInterviewScheduleRepository {

    private final StaffInterviewHostSlotJpaRepository hostSlotJpaRepository;
    private final StaffInterviewAssignmentJpaRepository assignmentJpaRepository;

    @Override
    public List<StaffInterviewHostSlot> findHostSlots(Integer recruitmentId) {
        return hostSlotJpaRepository.findAllByRecruitmentId(recruitmentId);
    }

    @Override
    public void saveHostSlots(List<StaffInterviewHostSlot> slots) {
        hostSlotJpaRepository.saveAll(slots);
    }

    @Override
    public void deleteHostSlots(List<StaffInterviewHostSlot> slots) {
        hostSlotJpaRepository.deleteAll(slots);
    }

    @Override
    public List<StaffInterviewAssignment> findAssignments(Integer recruitmentId) {
        return assignmentJpaRepository.findAllByRecruitmentId(recruitmentId);
    }

    @Override
    public Optional<StaffInterviewAssignment> findAssignmentByApplicationId(Integer applicationId) {
        return assignmentJpaRepository.findByApplicationId(applicationId);
    }

    @Override
    public StaffInterviewAssignment saveAssignment(StaffInterviewAssignment assignment) {
        return assignmentJpaRepository.save(assignment);
    }

    @Override
    public void deleteAssignment(StaffInterviewAssignment assignment) {
        assignmentJpaRepository.delete(assignment);
    }

    @Override
    public void flush() {
        assignmentJpaRepository.flush();
    }
}
