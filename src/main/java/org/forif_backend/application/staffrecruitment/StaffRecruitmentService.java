package org.forif_backend.application.staffrecruitment;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.staffrecruitment.dto.CreateStaffRecruitmentCommand;
import org.forif_backend.application.staffrecruitment.dto.StaffRecruitmentDepartmentCommand;
import org.forif_backend.application.staffrecruitment.dto.StaffRecruitmentDetail;
import org.forif_backend.application.staffrecruitment.dto.StaffRecruitmentQuestionCommand;
import org.forif_backend.application.staffrecruitment.dto.UpdateStaffRecruitmentCommand;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staffrecruitment.StaffApplicationRepository;
import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentDepartmentSpec;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentQuestionSpec;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentRepository;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 운영진 모집 회차 관리·조회.
 *
 * 접수 가능 여부 판정은 도메인(StaffRecruitment)에만 두고, 관리 API와 공개 API가 같은 규칙을 쓴다.
 * 권한 검사는 기존 방식대로 web 계층에서 StaffAccountService로 수행한다.
 */
@Service
@RequiredArgsConstructor
public class StaffRecruitmentService {

    private final StaffRecruitmentRepository staffRecruitmentRepository;
    private final StaffApplicationRepository staffApplicationRepository;
    private final SemesterService semesterService;

    // ==================== 회장단 관리 ====================

    @Transactional
    public StaffRecruitmentDetail create(CreateStaffRecruitmentCommand command) {
        SemesterInfo active = semesterService.getActive();
        int actYear = command.actYear() != null ? command.actYear() : active.actYear();
        int actSemester = command.actSemester() != null ? command.actSemester() : active.actSemester();

        StaffRecruitment recruitment = StaffRecruitment.create(
                actYear, actSemester, command.title(), command.summary(), command.description(),
                command.startsAt(), command.endsAt());
        recruitment.mergeDepartments(toDepartmentSpecs(command.departments()), false);
        recruitment.mergeQuestions(toQuestionSpecs(command.questions()), false);

        StaffRecruitment saved = staffRecruitmentRepository.save(recruitment);
        flushOrConflict();
        return detail(saved);
    }

    @Transactional
    public StaffRecruitmentDetail update(Integer recruitmentId, UpdateStaffRecruitmentCommand command) {
        StaffRecruitment recruitment = findForUpdate(recruitmentId);
        requireVersion(recruitment, command.version());

        recruitment.updateDetails(command.title(), command.summary(), command.description(),
                command.startsAt(), command.endsAt());
        // 제출이 생긴 뒤에는 부서·문항을 지울 수 없다. 내용 수정과 숨김은 그대로 허용한다.
        boolean hasApplications = staffApplicationRepository.existsByRecruitmentId(recruitmentId);
        if (command.departments() != null) {
            recruitment.mergeDepartments(toDepartmentSpecs(command.departments()), hasApplications);
        }
        if (command.questions() != null) {
            recruitment.mergeQuestions(toQuestionSpecs(command.questions()), hasApplications);
        }

        flushOrConflict();
        return detail(recruitment);
    }

    @Transactional
    public StaffRecruitmentDetail publish(Integer recruitmentId, Long version) {
        StaffRecruitment recruitment = findForUpdate(recruitmentId);
        requireVersion(recruitment, version);

        staffRecruitmentRepository.findPublished()
                .filter(published -> !Objects.equals(published.getId(), recruitment.getId()))
                .ifPresent(published -> {
                    throw new ForifException(ErrorCode.STAFF_RECRUITMENT_ACTIVE_CONFLICT);
                });

        recruitment.publish();
        // 동시에 두 요청이 게시를 시도하면 published_slot 유니크 제약이 최종 방어선이다.
        try {
            staffRecruitmentRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ForifException(ErrorCode.STAFF_RECRUITMENT_ACTIVE_CONFLICT);
        }
        return detail(recruitment);
    }

    @Transactional
    public StaffRecruitmentDetail close(Integer recruitmentId, String reason, Long version) {
        StaffRecruitment recruitment = findForUpdate(recruitmentId);
        requireVersion(recruitment, version);
        recruitment.close(reason, LocalDateTime.now());
        flushOrConflict();
        return detail(recruitment);
    }

    @Transactional
    public StaffRecruitmentDetail cancel(Integer recruitmentId, String reason, Long version) {
        StaffRecruitment recruitment = findForUpdate(recruitmentId);
        requireVersion(recruitment, version);
        recruitment.cancel(reason, LocalDateTime.now());
        flushOrConflict();
        return detail(recruitment);
    }

    @Transactional(readOnly = true)
    public StaffRecruitmentDetail getForAdmin(Integer recruitmentId) {
        return detail(findDetail(recruitmentId));
    }

    /** 어드민 목록. 초안을 포함하며 정렬은 recruitment_id DESC 고정이다. */
    @Transactional(readOnly = true)
    public CursorPageResponse<StaffRecruitmentDetail> getAdminList(StaffRecruitmentStatus status, int page, int size) {
        long totalElements = staffRecruitmentRepository.countForAdmin(status);
        LocalDateTime now = LocalDateTime.now();
        List<StaffRecruitmentDetail> content = staffRecruitmentRepository
                .findAllForAdmin(status, page * size, size).stream()
                .map(recruitment -> StaffRecruitmentDetail.from(recruitment, now))
                .toList();
        boolean hasNext = (long) (page + 1) * size < totalElements;
        return CursorPageResponse.ofOffset(content, hasNext, totalElements, page, size);
    }

    /** 지정한 회차들의 관리 상세. 열람 권한이 있는 회차 목록을 만들 때 쓴다. */
    @Transactional(readOnly = true)
    public List<StaffRecruitmentDetail> getByIds(List<Integer> recruitmentIds) {
        LocalDateTime now = LocalDateTime.now();
        return recruitmentIds.stream()
                .map(staffRecruitmentRepository::findDetailById)
                .flatMap(Optional::stream)
                .map(recruitment -> StaffRecruitmentDetail.from(recruitment, now))
                .toList();
    }

    // ==================== 공개 조회 ====================

    /** 현재 게시 중인 회차. 없으면 빈 값이며 공개 응답은 data:null이다. */
    @Transactional(readOnly = true)
    public Optional<StaffRecruitmentDetail> getPublicCurrent() {
        return staffRecruitmentRepository.findPublished()
                .filter(StaffRecruitment::isPubliclyVisible)
                .map(this::detail);
    }

    /** 공개 상세. 초안과 일정 미설정 회차는 존재를 드러내지 않고 404다. */
    @Transactional(readOnly = true)
    public StaffRecruitmentDetail getPublicDetail(Integer recruitmentId) {
        StaffRecruitment recruitment = findDetail(recruitmentId);
        if (!recruitment.isPubliclyVisible()) {
            throw new ForifException(ErrorCode.STAFF_RECRUITMENT_NOT_FOUND);
        }
        return detail(recruitment);
    }

    // ==================== 내부 ====================

    private StaffRecruitment findForUpdate(Integer recruitmentId) {
        return staffRecruitmentRepository.findByIdForUpdate(recruitmentId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_RECRUITMENT_NOT_FOUND));
    }

    private StaffRecruitment findDetail(Integer recruitmentId) {
        return staffRecruitmentRepository.findDetailById(recruitmentId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_RECRUITMENT_NOT_FOUND));
    }

    private StaffRecruitmentDetail detail(StaffRecruitment recruitment) {
        return StaffRecruitmentDetail.from(recruitment, LocalDateTime.now());
    }

    /** 화면이 들고 있던 version이 낡았으면 덮어쓰지 않고 409로 돌려보낸다. */
    private void requireVersion(StaffRecruitment recruitment, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(recruitment.getVersion())) {
            throw new ForifException(ErrorCode.DATA_INTEGRITY_VIOLATION);
        }
    }

    private void flushOrConflict() {
        try {
            staffRecruitmentRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ForifException(ErrorCode.DATA_INTEGRITY_VIOLATION);
        }
    }

    private List<StaffRecruitmentDepartmentSpec> toDepartmentSpecs(List<StaffRecruitmentDepartmentCommand> commands) {
        if (commands == null) {
            return List.of();
        }
        return commands.stream()
                .map(command -> new StaffRecruitmentDepartmentSpec(
                        command.id(), command.name(), command.description(),
                        command.displayOrder(), command.active()))
                .toList();
    }

    private List<StaffRecruitmentQuestionSpec> toQuestionSpecs(List<StaffRecruitmentQuestionCommand> commands) {
        if (commands == null) {
            return List.of();
        }
        return commands.stream()
                .map(command -> new StaffRecruitmentQuestionSpec(
                        command.id(), command.label(), command.description(), command.type(),
                        command.required(), command.maxLength(), command.displayOrder()))
                .toList();
    }
}
