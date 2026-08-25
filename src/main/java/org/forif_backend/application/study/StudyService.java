package org.forif_backend.application.study;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.file.dto.FileInfo;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.staff.dto.CreateMentorCommand;
import org.forif_backend.application.study.dto.*;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.common.type.SortCriteria;
import org.forif_backend.application.semester.SemesterPhaseGuard;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.common.util.DateUtils;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;
import org.forif_backend.domain.study.*;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApplyRepository;
import org.forif_backend.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.forif_backend.application.file.FileViewUrls;
import org.forif_backend.application.file.TransactionalFileCleanup;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyService {

    private static final String FILE_CLEANUP_CONTEXT = "스터디 수정 중";

    // TODO: 하드코딩된 기본 비밀번호 개선 필요. 멘토가 직접 초기 비밀번호를 설정하거나,
    //       랜덤 생성 후 이메일 발송하는 방식으로 변경 필요.

    private final SemesterService semesterService;
    private final SemesterPhaseGuard semesterPhaseGuard;
    private final StudyRecruitStatusPolicy recruitStatusPolicy;
    private final StudyMentorAccess studyMentorAccess;
    private final StudyRepository studyRepository;
    private final StudyUserRepository studyUserRepository;
    private final StudyAttendanceRepository studyAttendanceRepository;
    private final UserRepository userRepository;
    private final UserApplyRepository userApplyRepository;
    private final FilePort filePort;
    private final StaffAccountService staffAccountService;
    private final StaffAccountRepository staffAccountRepository;
    private final MentorConfirmationRepository mentorConfirmationRepository;

    @Transactional(readOnly = true)
    public CursorPageResponse<StudyDto> getStudies(Integer cursor, Integer page, int size, Integer year, Integer semester,
                                     List<StudyDifficulty> difficulties, List<String> tags,
                                     RecruitStatus recruitStatus, String search) {

        StudySearchCond searchCond = StudySearchCond.builder()
            .year(year)
            .semester(semester)
            .difficulties(difficulties)
            .studyTagNames(tags)
            .recruitStatus(recruitStatus)
            .searchKeyword(search)
            .build();

        long totalElements = studyRepository.countStudiesForUser(searchCond);

        CursorPageResponse<Study> studies = CursorPageResponse.paginate(
                page, size, totalElements,
                () -> studyRepository.getStudiesWithOffset(searchCond, page, size),
                () -> studyRepository.getStudies(searchCond, cursor, size),
                Study::getId);

        return studies.withContent(studies.content().stream().map(this::toStudyDto).toList());
    }

    @Transactional(readOnly = true)
    public List<StudyDto> getMyCreatedStudies(Long mentorId) {

        return studyRepository.findStudiesByMentorId(mentorId)
            .stream()
            .map(this::toStudyDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<StudyApplicationDto> getMyStudyApplications(Long mentorId) {
        SemesterInfo activeSemester = semesterService.getActive();
        return studyRepository.findStudyApplicationsByMentorId(
                        mentorId,
                        activeSemester.actYear(),
                        activeSemester.actSemester()
                )
                .stream()
                .filter(study -> !study.isAutonomousStudy())
                .map(study -> StudyApplicationDto.from(
                        study,
                        canModifyStudyApplication(study),
                        canCancelStudyApplication(study)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudyApplicationDetailDto getMyStudyApplication(Long mentorId, Integer studyId) {
        Study study = studyRepository.findStudyByIdWithTags(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        SemesterInfo activeSemester = semesterService.getActive();
        boolean isApplicationStatus = study.getStudyStatus() == StudyStatus.PENDING
                || study.getStudyStatus() == StudyStatus.RE_APPLIED
                || study.getStudyStatus() == StudyStatus.REJECTED
                || (study.getStudyStatus() == StudyStatus.APPROVED
                && activeSemester.matches(study.getActYear(), study.getActSemester()));
        if (!study.isMentor(mentorId)
                || study.isAutonomousStudy()
                || !isApplicationStatus) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }

        return StudyApplicationDetailDto.builder()
                .study(getStudyDetail(studyId))
                .studyStatus(study.getStudyStatus())
                .rejectReason(study.getRejectReason())
                .canModify(canModifyStudyApplication(study))
                .canCancel(canCancelStudyApplication(study))
                .build();
    }

    @Transactional(readOnly = true)
    public UserStudiesResult getUserStudies(Long userId) {
        // 1. userId로 스터디 목록 조회 (이미 연도, 학기 내림차순으로 정렬됨)
        List<Study> studies = studyRepository.findStudiesByUserId(userId);

        // 스터디별 수료증 발급 여부 (마이페이지 다운로드 버튼 활성화 판단용)
        Map<Integer, Boolean> certificateIssuedMap = studyUserRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(
                        su -> su.getStudy().getId(),
                        su -> su.getCertificateStatus() != null && su.getCertificateStatus() == 1,
                        (a, b) -> a || b
                ));

        // 2. 현재 학기 정보
        SemesterInfo active = semesterService.getActive();
        int currentYear = active.actYear();
        int currentSemester = active.actSemester();

        // 3. SemesterStudiesInfo 리스트 생성 (한 학기에 스터디 1개만)
        List<SemesterStudiesInfo> semesters = studies.stream()
                .collect(Collectors.groupingBy(
                        study -> study.getActYear() + "-" + study.getActSemester(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                studyList -> {
                                    Study firstStudy = studyList.get(0);
                                    int year = firstStudy.getActYear();
                                    int semester = firstStudy.getActSemester();

                                    return SemesterStudiesInfo.builder()
                                            .year(year)
                                            .semester(semester)
                                            .semesterLabel(year + "-" + semester)
                                            .isCurrent(year == currentYear && semester == currentSemester)
                                            .study(toStudyInfo(firstStudy,
                                                    certificateIssuedMap.getOrDefault(firstStudy.getId(), false)))  // 첫 번째 스터디만 (한 학기에 1개)
                                            .build();
                                }
                        )
                ))
                .values()
                .stream()
                .sorted((s1, s2) -> {
                    // 연도 내림차순, 같으면 학기 내림차순
                    int yearCompare = s2.year().compareTo(s1.year());
                    if (yearCompare != 0) return yearCompare;
                    return s2.semester().compareTo(s1.semester());
                })
                .toList();

        // 6. UserStudiesResult 반환
        return UserStudiesResult.builder()
                .semesters(semesters)
                .build();
    }

    @Transactional(readOnly = true)
    public StudyDetailDto getStudyDetail(Integer studyId) {
        Study study = studyRepository.findStudyByIdWithTags(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        List<StudyPlan> plans = studyRepository.findStudyPlansByStudyId(studyId);
        List<StudyReference> references = studyRepository.findStudyReferencesByStudyId(studyId);
        List<MentorStudy> mentorStudies = studyRepository.findMentorStudiesByStudyId(studyId);
        Map<UUID, String> referenceContents = references.stream()
                .collect(Collectors.toMap(StudyReference::getId, this::resolveReferenceContent));

        return StudyDetailDto.of(
                study,
                plans,
                references,
                mentorStudies,
                resolveThumbnailImage(study),
                referenceContents
        );
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<AdminStudyDto> getAdminStudies(Integer cursor, Integer page, int size, Integer year, Integer semester, String search, List<StudyStatus> studyStatuses, List<SortCriteria> sorting) {
        List<StudyStatus> statusFilter = studyStatuses == null || studyStatuses.isEmpty()
                ? List.of(StudyStatus.APPROVED, StudyStatus.STARTED)
                : studyStatuses;
        long totalElements = studyRepository.countStudies(year, semester, search, statusFilter);

        CursorPageResponse<Study> studies = CursorPageResponse.paginate(
                page, size, totalElements,
                () -> studyRepository.searchAdminStudiesWithOffset(page, size, year, semester, search, statusFilter, sorting),
                () -> studyRepository.searchStudiesWithCursor(cursor, size, year, semester, search, statusFilter),
                Study::getId);

        List<Integer> studyIds = studies.content().stream().map(Study::getId).toList();
        Map<Integer, Long> menteeCountMap = studyRepository.countMenteesByStudyIds(studyIds);
        return studies.withContent(studies.content().stream()
                .map(s -> AdminStudyDto.of(s, menteeCountMap.getOrDefault(s.getId(), 0L)))
                .toList());
    }

    @Transactional
    public void updateStudy(Integer studyId, UpdateStudyCommand request) {
        updateStudy(studyId, request, null, List.of(), true, true);
    }

    @Transactional
    public void updateStudy(Integer studyId, UpdateStudyCommand request,
                            MultipartFile thumbnail, List<MultipartFile> referenceFiles) {
        updateStudy(studyId, request, thumbnail,
                Optional.ofNullable(referenceFiles).orElseGet(Collections::emptyList), false, false);
    }

    private void updateStudy(Integer studyId, UpdateStudyCommand request,
                             MultipartFile thumbnail, List<MultipartFile> referenceFiles,
                             boolean skipUnuploadedFileReferences,
                             boolean retainExistingFilesWhenRetainedIdsOmitted) {
        Study study = studyRepository.findStudyByIdWithTags(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        applyStudyUpdate(studyId, study, request);
        if (request.getReferences() != null) {
            replaceStudyReferences(
                    studyId,
                    study,
                    request.getReferences(),
                    request.getRetainedReferenceIds(),
                    referenceFiles,
                    skipUnuploadedFileReferences,
                    retainExistingFilesWhenRetainedIdsOmitted
            );
        }

        if (thumbnail != null && !thumbnail.isEmpty()) {
            String previousThumbnailObjectKey = study.getThumbnailImage();
            FileInfo thumbnailInfo = uploadAndBuildFileInfo(thumbnail);
            study.setThumbnailImage(thumbnailInfo.objectKey());
            deleteStoredFilesAfterCompletion(
                    Collections.singletonList(previousThumbnailObjectKey),
                    List.of(thumbnailInfo.objectKey())
            );
        }
        studyRepository.saveStudy(study);
    }

    private void applyStudyUpdate(Integer studyId, Study study, UpdateStudyCommand request) {
        // null이 아닌 기본 필드만 반영
        if (!study.isAutonomousStudy()
                && Study.isAutonomousStudyName(request.getStudyName())) {
            throw new ForifException(ErrorCode.AUTONOMOUS_STUDY_NAME_RESERVED);
        }
        if (study.isAutonomousStudy()
                && request.getStudyName() != null
                && !Study.AUTONOMOUS_STUDY_NAME.equals(request.getStudyName())) {
            throw new ForifException(ErrorCode.AUTONOMOUS_STUDY_NAME_NOT_CHANGEABLE);
        }
        if (request.getStudyName() != null) study.setStudyName(request.getStudyName());
        if (request.getOneLiner() != null) study.setOneLiner(request.getOneLiner());
        if (request.getExplanation() != null) study.setExplanation(request.getExplanation());
        if (request.getStartTime() != null) study.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) study.setEndTime(request.getEndTime());
        if (request.getWeekDay() != null) study.setWeekDay(request.getWeekDay());
        if (request.getLocation() != null) study.setLocation(request.getLocation());
        if (request.getLocationDetail() != null) study.setLocationDetail(request.getLocationDetail());
        if (request.getIsOnline() != null) study.setIsOnline(request.getIsOnline());
        if (request.getCapacity() != null) study.setCapacity(request.getCapacity());
        if (request.getSelectionCriteria() != null) study.setSelectionCriteria(request.getSelectionCriteria());
        if (request.isSecondaryMentorIdPresent()) {
            User secondaryMentor = request.getSecondaryMentorId() == null
                    ? null
                    : resolveSecondaryMentor(
                    study.getPrimaryMentor().getId(), request.getSecondaryMentorId());
            study.setSecondaryMentor(secondaryMentor);
            study.setSecondaryMentorName(secondaryMentor == null ? null : secondaryMentor.getUserName());
        }
        if (request.getRequiresInterview() != null) study.setRequiresInterview(request.getRequiresInterview());
        if (Boolean.FALSE.equals(request.getRequiresInterview())) {
            study.setInterviewDate(null);
        } else if (request.getInterviewDate() != null) {
            study.setInterviewDate(request.getInterviewDate());
        }

        // enum 변환 필드
        if (request.getDifficulty() != null) {
            study.setDifficulty(StudyDifficulty.fromLevel(request.getDifficulty()));
        }

        // 태그 교체
        if (request.getStudyTagIds() != null || request.getStudyTagNames() != null) {
            study.setTags(resolveStudyTags(request.getStudyTagIds(), request.getStudyTagNames()));
        }

        // 커리큘럼: 기존 삭제 후 재생성
        if (request.getStudyPlanList() != null) {
            studyRepository.deleteStudyPlansByStudyId(studyId);
            List<StudyPlan> plans = request.getStudyPlanList().stream()
                    .map(plan -> StudyPlan.create(
                            plan.getWeekNum(), plan.getDate(), plan.getTopic(), plan.getContent(), study))
                    .toList();
            studyRepository.saveAllStudyPlan(plans);
        }

    }

    @Transactional
    public void deleteStudy(Integer studyId) {
        studyRepository.findStudyById(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        studyRepository.deleteStudyPlansByStudyId(studyId);
        studyRepository.deleteStudyReferencesByStudyId(studyId);
        studyRepository.deleteStudyUsersByStudyId(studyId);
        studyRepository.deleteMentorStudiesByStudyId(studyId);
        mentorConfirmationRepository.deleteByStudyId(studyId);
        studyRepository.deleteStudyById(studyId);
    }

    /**
     * 멘토가 승인 전 스터디 개설 신청을 취소한다.
     */
    @Transactional
    public void cancelStudyApplication(Integer studyId, Long userId) {
        Study study = studyRepository.findStudyById(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        studyMentorAccess.requireMentorOfActiveSemester(study, userId);
        if (study.getStudyStatus() == StudyStatus.APPROVED
                || study.getStudyStatus() == StudyStatus.STARTED) {
            throw new ForifException(ErrorCode.STUDY_ALREADY_APPROVED);
        }
        semesterPhaseGuard.requireOpen(SemesterPhase.MENTOR_RECRUIT);

        // 승인 전 스터디에는 지원서·수강생·출석이 없어야 정상이다. 있는데 조용히 지우면
        // 멘티 기록이 소리 없이 사라지므로, 취소를 막고 운영진 확인을 거치게 한다.
        if (hasStudyApplicationCancellationDependencies(studyId)) {
            throw new ForifException(ErrorCode.STUDY_CANCEL_HAS_DEPENDENTS);
        }

        deleteStoredFiles(study);

        studyRepository.deleteStudyPlansByStudyId(studyId);
        studyRepository.deleteStudyReferencesByStudyId(studyId);
        studyRepository.deleteMentorStudiesByStudyId(studyId);
        studyRepository.deleteStudyById(studyId);
    }

    /** 취소된 스터디의 썸네일·첨부 파일을 스토리지에서 걷어낸다 */
    private void deleteStoredFiles(Study study) {
        List<String> objectKeys = new ArrayList<>();
        if (study.getThumbnailImage() != null && !study.getThumbnailImage().isBlank()) {
            objectKeys.add(study.getThumbnailImage());
        }
        studyRepository.findStudyReferencesByStudyId(study.getId()).stream()
                .filter(ref -> ref.getReferenceType() == ReferenceType.FILE)
                .map(StudyReference::getContent)
                .filter(Objects::nonNull)
                .forEach(objectKeys::add);

        for (String objectKey : objectKeys) {
            try {
                filePort.deleteFile(objectKey);
            } catch (Exception e) {
                // 파일 삭제 실패가 취소 자체를 막을 이유는 없다. 남은 파일은 따로 정리한다
                log.warn("스터디 취소 중 파일 삭제 실패: {}", objectKey, e);
            }
        }
    }

    /**
     * 스터디 개설 신청 저장 메서드입니다.
     * @param mentorId 개설 신청하는 유저 id
     * @param request 신청 정보
     * @param thumbnail 썸네일 이미지 파일 정보
     * @param referenceFiles 참고 자료 파일 정보.
     * @return 서버에 저장된 파일의 조회 URL 정보
     */
    @Transactional
    public CreateStudyApplyInfo createStudyApply(Long mentorId, CreateStudyApplyCommand request,
                                                 MultipartFile thumbnail, List<MultipartFile> referenceFiles) {
        semesterPhaseGuard.requireOpen(SemesterPhase.MENTOR_RECRUIT);
        requireRegularStudyName(request.getTitle());

        User mentor = userRepository.findUserById(mentorId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        // 생성 시점에 필요한 최소한의 정보만 주입
        SemesterInfo semester = semesterService.getActive();
        Study study = Study.createPendingStudy(mentor, semester.actYear(), semester.actSemester());

        List<StudyTag> tags = resolveStudyTags(request);
        User secondaryMentor = resolveSecondaryMentor(mentorId, request.getSecondaryMentorId());

        // 공통 데이터 반영
        study.applyRequestData(request.toApplyData(), tags, secondaryMentor);

        return saveStudyWithResources(study, request, thumbnail, referenceFiles);
    }

    private List<StudyTag> resolveStudyTags(CreateStudyApplyCommand request) {
        return resolveStudyTags(request.getStudyTagId(), request.getStudyTagNames());
    }

    private List<StudyTag> resolveStudyTags(List<Long> tagIds, List<String> tagNames) {
        if (tagNames != null && !tagNames.isEmpty()) {
            List<String> normalizedTagNames = tagNames.stream()
                    .map(StudyService::normalizeTagName)
                    .distinct()
                    .toList();
            List<StudyTag> tags = studyRepository.findAllStudyTagByName(normalizedTagNames);
            if (tags.size() != normalizedTagNames.size()) {
                throw new ForifException(ErrorCode.INVALID_INPUT);
            }
            return tags;
        }

        if (tagIds != null && !tagIds.isEmpty()) {
            List<Long> distinctTagIds = tagIds.stream()
                    .distinct()
                    .toList();
            List<StudyTag> tags = studyRepository.findAllStudyTagById(distinctTagIds);
            if (tags.size() != distinctTagIds.size()) {
                throw new ForifException(ErrorCode.INVALID_INPUT);
            }
            return tags;
        }

        // 수정 요청에서 명시적으로 빈 목록을 보내면 기존 태그를 모두 해제한다.
        return List.of();
    }

    private static String normalizeTagName(String tagName) {
        if (tagName == null || tagName.isBlank()) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
        return tagName.strip().toLowerCase(Locale.ROOT);
    }

    private User resolveSecondaryMentor(Long primaryMentorId, Long secondaryMentorId) {
        if (secondaryMentorId == null) {
            return null;
        }

        if (primaryMentorId.equals(secondaryMentorId)) {
            throw new ForifException(ErrorCode.BAD_REQUEST);
        }

        return userRepository.findUserById(secondaryMentorId)
                .orElseThrow(() -> new ForifException(ErrorCode.SECOND_MENTOR_NOT_FOUND));
    }

    /**
     * StudyPlan 생성
     */
    private StudyPlan createStudyPlan(CreateStudyApplyCommand.Plan planRequest, Study study) {
        return StudyPlan.create(
                planRequest.getWeekNum(),
                planRequest.getDate(),
                planRequest.getTopic(),
                planRequest.getContent(),
                study
        );
    }

    /**
     * Reference DTO를 StudyReference 엔티티로 변환하고,
     * 파일 타입일 경우 저장된 파일 정보를 'referenceUploadInfos' 리스트에 추가합니다.
     */
    private StudyReference toReferenceEntity(CreateStudyApplyCommand.Reference reference, Study study, List<MultipartFile> referenceFiles, List<FileInfo> referenceUploadInfos) {
        String content;
        // ReferenceType을 study 패키지의 것으로 변환
        ReferenceType refType = ReferenceType.valueOf(reference.getType().name());

        if (refType == ReferenceType.FILE) { // 참고자료 타입이 파일인 경우
            // 이름에 맞는 파일 찾기
            MultipartFile file = referenceFiles.stream().filter(referenceFile -> Objects.equals(referenceFile.getOriginalFilename(), reference.getFileName())).findAny()
                    .orElseThrow(() -> new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT));

            FileInfo fileInfo = uploadAndBuildFileInfo(file);

            // 반환값에 추가
            referenceUploadInfos.add(fileInfo);

            content = fileInfo.objectKey();
        } else { // 참고자료 타입이 url인 경우
            content = reference.getUrl();
        }
        return StudyReference.create(study, refType, content);
    }

    /**
     * 거절된 스터디 수정 후 재요청
     * @return 서버에 저장된 파일의 조회 URL 정보가 담긴 Info 객체
     */
    @Transactional
    public CreateStudyApplyInfo reApplyStudy(Integer studyId, Long userId, CreateStudyApplyCommand request,
                                             MultipartFile thumbnail, List<MultipartFile> referenceFiles) {
        return updateStudyApplication(studyId, userId, request, thumbnail, referenceFiles, true);
    }

    @Transactional
    public CreateStudyApplyInfo updateStudyApplication(Integer studyId, Long userId, UpdateStudyCommand request,
                                                        MultipartFile thumbnail, List<MultipartFile> referenceFiles) {
        Study study = findModifiableStudyApplication(studyId, userId, false);
        applyStudyUpdate(studyId, study, request);

        List<FileInfo> referenceUploadInfos = request.getReferences() == null
                ? List.of()
                : replaceStudyReferences(
                        studyId,
                        study,
                        request.getReferences(),
                        request.getRetainedReferenceIds(),
                        Optional.ofNullable(referenceFiles).orElseGet(Collections::emptyList),
                        false,
                        false
                );

        FileInfo thumbnailInfo = null;
        if (thumbnail != null && !thumbnail.isEmpty()) {
            String previousThumbnailObjectKey = study.getThumbnailImage();
            thumbnailInfo = uploadAndBuildFileInfo(thumbnail);
            study.setThumbnailImage(thumbnailInfo.objectKey());
            deleteStoredFilesAfterCompletion(
                    Collections.singletonList(previousThumbnailObjectKey),
                    List.of(thumbnailInfo.objectKey())
            );
        }

        studyRepository.saveStudy(study);

        return CreateStudyApplyInfo.builder()
                .studyId(study.getId())
                .thumbnailUploadInfo(thumbnailInfo)
                .referenceUploadInfos(referenceUploadInfos)
                .build();
    }

    private List<FileInfo> replaceStudyReferences(
            Integer studyId,
            Study study,
            List<UpdateStudyCommand.Reference> newReferences,
            List<UUID> retainedReferenceIds,
            List<MultipartFile> referenceFiles,
            boolean skipUnuploadedFileReferences,
            boolean retainExistingFilesWhenRetainedIdsOmitted
    ) {
        List<StudyReference> existingReferences = studyRepository.findStudyReferencesByStudyId(studyId);
        Map<UUID, StudyReference> existingById = existingReferences.stream()
                .collect(Collectors.toMap(StudyReference::getId, reference -> reference));
        // 구형 어드민 JSON 클라이언트만 retained_reference_ids 생략 시 FILE 참고자료를 유지한다.
        Set<UUID> retainedIds = retainedReferenceIds == null && retainExistingFilesWhenRetainedIdsOmitted
                ? existingReferences.stream()
                .filter(reference -> reference.getReferenceType() == ReferenceType.FILE)
                .map(StudyReference::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                : new LinkedHashSet<>(Optional.ofNullable(retainedReferenceIds).orElseGet(Collections::emptyList));

        if (!existingById.keySet().containsAll(retainedIds)) {
            throw new ForifException(ErrorCode.BAD_REQUEST);
        }

        List<StudyReference> removedReferences = existingReferences.stream()
                .filter(reference -> !retainedIds.contains(reference.getId()))
                .toList();
        List<StudyReference> referencesToCreate = new ArrayList<>();

        List<FileInfo> uploadedFiles = new ArrayList<>();
        for (UpdateStudyCommand.Reference reference : newReferences) {
            if (reference.getType() == ReferenceType.FILE) {
                MultipartFile file = referenceFiles.stream()
                        .filter(candidate -> Objects.equals(candidate.getOriginalFilename(), reference.getFileName()))
                        .findFirst()
                        .orElse(null);
                if (file != null) {
                    FileInfo fileInfo = uploadAndBuildFileInfo(file);
                    uploadedFiles.add(fileInfo);
                    referencesToCreate.add(StudyReference.create(study, ReferenceType.FILE, fileInfo.objectKey()));
                } else if (!skipUnuploadedFileReferences) {
                    throw new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT);
                }
                // JSON 어드민 수정은 조회용 FILE URL을 저장하지 않는다. 기존 FILE은 retained 정책으로 유지된다.
            } else {
                referencesToCreate.add(StudyReference.create(study, reference.getType(), reference.getUrl()));
            }
        }

        if (!removedReferences.isEmpty()) {
            studyRepository.deleteStudyReferencesByIds(removedReferences.stream()
                    .map(StudyReference::getId)
                    .toList());
        }
        if (!referencesToCreate.isEmpty()) {
            studyRepository.saveAllStudyReference(referencesToCreate);
        }
        deleteStoredFilesAfterCompletion(
                removedReferences.stream()
                        .filter(reference -> reference.getReferenceType() == ReferenceType.FILE)
                        .map(StudyReference::getContent)
                        .toList(),
                uploadedFiles.stream().map(FileInfo::objectKey).toList()
        );
        return uploadedFiles;
    }

    /** DB 반영 성공 뒤 교체 전 파일을 지우고, 롤백 시 새로 올린 파일을 회수한다. */
    private void deleteStoredFilesAfterCompletion(List<String> previousObjectKeys, List<String> uploadedObjectKeys) {
        TransactionalFileCleanup.replaceAfterCompletion(
                filePort, previousObjectKeys, uploadedObjectKeys, FILE_CLEANUP_CONTEXT);
    }

    private void deleteStoredFilesQuietly(List<String> objectKeys) {
        TransactionalFileCleanup.deleteQuietly(filePort, objectKeys, FILE_CLEANUP_CONTEXT);
    }

    private CreateStudyApplyInfo updateStudyApplication(Integer studyId, Long userId, CreateStudyApplyCommand request,
                                                         MultipartFile thumbnail, List<MultipartFile> referenceFiles,
                                                         boolean rejectedOnly) {
        requireRegularStudyName(request.getTitle());
        Study study = findModifiableStudyApplication(studyId, userId, rejectedOnly);

        List<StudyTag> tags = resolveStudyTags(request);
        User secondaryMentor = resolveSecondaryMentor(study.getPrimaryMentor().getId(), request.getSecondaryMentorId());
        study.applyRequestData(request.toApplyData(), tags, secondaryMentor);

        if (request.getStudyPlanList() != null) {
            studyRepository.deleteStudyPlansByStudyId(studyId);
        }
        List<String> previousReferenceObjectKeys = List.of();
        if (request.getReferences() != null) {
            previousReferenceObjectKeys = studyRepository.findStudyReferencesByStudyId(studyId).stream()
                    .filter(reference -> reference.getReferenceType() == ReferenceType.FILE)
                    .map(StudyReference::getContent)
                    .toList();
            studyRepository.deleteStudyReferencesByStudyId(studyId);
        }

        String previousThumbnailObjectKey = thumbnail != null && !thumbnail.isEmpty()
                ? study.getThumbnailImage()
                : null;
        CreateStudyApplyInfo result = saveStudyWithResources(study, request, thumbnail, referenceFiles);
        List<String> previousObjectKeys = new ArrayList<>(previousReferenceObjectKeys);
        previousObjectKeys.add(previousThumbnailObjectKey);
        List<String> uploadedObjectKeys = new ArrayList<>(result.referenceUploadInfos().stream()
                .map(FileInfo::objectKey)
                .toList());
        if (result.thumbnailUploadInfo() != null) {
            uploadedObjectKeys.add(result.thumbnailUploadInfo().objectKey());
        }
        deleteStoredFilesAfterCompletion(previousObjectKeys, uploadedObjectKeys);

        return result;
    }

    private Study findModifiableStudyApplication(Integer studyId, Long userId, boolean rejectedOnly) {
        Study study = studyRepository.findStudyByIdWithTags(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        studyMentorAccess.requireMentorOfActiveSemester(study, userId);
        if (study.isAutonomousStudy()) {
            throw new ForifException(ErrorCode.AUTONOMOUS_STUDY_APPLICATION_NOT_ALLOWED);
        }
        StudyStatus studyStatus = study.getStudyStatus();

        if (rejectedOnly && studyStatus != StudyStatus.REJECTED) {
            throw new ForifException(ErrorCode.REAPPLY_ONLY_FOR_REJECTED);
        }
        if (studyStatus != StudyStatus.PENDING
                && studyStatus != StudyStatus.RE_APPLIED
                && studyStatus != StudyStatus.REJECTED
                && studyStatus != StudyStatus.APPROVED) {
            throw new ForifException(ErrorCode.BAD_REQUEST);
        }

        semesterPhaseGuard.requireBeforeStart(SemesterPhase.MENTEE_RECRUIT);

        if (studyStatus == StudyStatus.REJECTED) {
            // 반려 건을 수정하면 재신청 상태가 되므로, 심사 창이 닫힌 뒤에는
            // 처리할 수 없는 RE_APPLIED 신청서가 생기지 않게 재신청을 막는다.
            semesterPhaseGuard.requireOpen(SemesterPhase.MENTOR_REVIEW);
            study.reApply();
        }

        return study;
    }

    private boolean canModifyStudyApplication(Study study) {
        if (study.isAutonomousStudy()) {
            return false;
        }

        SemesterInfo active = semesterService.getActive();
        if (!active.matches(study.getActYear(), study.getActSemester())
                || !semesterPhaseGuard.isBeforeStart(
                SemesterPhase.MENTEE_RECRUIT,
                study.getActYear(),
                study.getActSemester()
        )) {
            return false;
        }

        if (study.getStudyStatus() == StudyStatus.REJECTED) {
            return semesterPhaseGuard.isOpen(
                    SemesterPhase.MENTOR_REVIEW,
                    study.getActYear(),
                    study.getActSemester()
            );
        }

        return study.getStudyStatus() == StudyStatus.PENDING
                || study.getStudyStatus() == StudyStatus.RE_APPLIED
                || study.getStudyStatus() == StudyStatus.APPROVED;
    }

    private boolean canCancelStudyApplication(Study study) {
        SemesterInfo active = semesterService.getActive();
        return (study.getStudyStatus() == StudyStatus.PENDING
                || study.getStudyStatus() == StudyStatus.RE_APPLIED
                || study.getStudyStatus() == StudyStatus.REJECTED)
                && active.matches(study.getActYear(), study.getActSemester())
                && semesterPhaseGuard.isOpen(
                SemesterPhase.MENTOR_RECRUIT,
                study.getActYear(),
                study.getActSemester()
        )
                && !hasStudyApplicationCancellationDependencies(study.getId());
    }

    private boolean hasStudyApplicationCancellationDependencies(Integer studyId) {
        return userApplyRepository.existsByStudyId(studyId)
                || !studyUserRepository.findAllByStudyId(studyId).isEmpty()
                || !studyAttendanceRepository.findAllByStudyId(studyId).isEmpty();
    }

    /**
     * [공통] 스터디 리소스(파일, 플랜, 참고자료) 처리 및 DB 저장
     */
    private CreateStudyApplyInfo saveStudyWithResources(Study study, CreateStudyApplyCommand request,
                                                        MultipartFile thumbnail, List<MultipartFile> referenceFiles) {
        FileInfo thumbnailInfo = null;
        // 썸네일 처리
        if (thumbnail != null && !thumbnail.isEmpty()) {
            thumbnailInfo = uploadAndBuildFileInfo(thumbnail);
            study.setThumbnailImage(thumbnailInfo.objectKey());
        }

        // 커리큘럼 생성 (null을 빈 리스트로 처리)
        List<StudyPlan> planList = Optional.ofNullable(request.getStudyPlanList())
                .orElseGet(Collections::emptyList)
                .stream()
                .map(plan -> createStudyPlan(plan, study))
                .toList();

        // 참고자료 생성 (null을 빈 리스트로 처리)
        List<FileInfo> referenceUploadInfos = new ArrayList<>();
        List<MultipartFile> safeReferenceFiles = Optional.ofNullable(referenceFiles)
                .orElseGet(Collections::emptyList);

        List<StudyReference> referenceList = Optional.ofNullable(request.getReferences())
                .orElseGet(Collections::emptyList) // null이면 빈 리스트 반환
                .stream()
                .map(ref -> toReferenceEntity(ref, study, safeReferenceFiles, referenceUploadInfos))
                .toList();

        // DB 저장
        studyRepository.saveStudy(study);
        studyRepository.saveAllStudyPlan(planList);
        studyRepository.saveAllStudyReference(referenceList);

        return CreateStudyApplyInfo.builder()
                .studyId(study.getId())
                .thumbnailUploadInfo(thumbnailInfo)
                .referenceUploadInfos(referenceUploadInfos)
                .build();
    }

    private FileInfo uploadAndBuildFileInfo(MultipartFile file) {
        String objectKey = filePort.uploadFile(file);
        return filePort.generatePresignedViewUrl(objectKey);
    }

    private StudyDto toStudyDto(Study study) {
        String viewUrl = resolveThumbnailImage(study);
        return viewUrl == null ? StudyDto.from(study) : StudyDto.from(study, viewUrl);
    }

    private StudyInfo toStudyInfo(Study study, boolean certificateIssued) {
        return StudyInfo.from(study, certificateIssued, resolveThumbnailImage(study));
    }

    private String resolveThumbnailImage(Study study) {
        return FileViewUrls.resolveViewUrl(filePort, study.getThumbnailImage());
    }

    private String resolveReferenceContent(StudyReference reference) {
        String content = reference.getContent();
        if (reference.getReferenceType() != ReferenceType.FILE) {
            return content;
        }
        String viewUrl = FileViewUrls.resolveViewUrl(filePort, content);
        return viewUrl != null ? viewUrl : content;
    }

    /**
     * [어드민 전용] 스터디 개설 승인
     */
    @Transactional
    public void approveStudy(Integer studyId) {
        semesterPhaseGuard.requireOpen(SemesterPhase.MENTOR_REVIEW);

        Study study = studyRepository.findStudyById(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));
        requireActiveSemesterStudy(study);

        study.approve();
        // 스케줄러가 30초마다 맞춰주지만 그 사이 모집 상태가 NULL로 남아 화면에서 "마감"으로
        // 보인다. 승인하자마자 옳은 값이 보이도록 여기서 먼저 채운다.
        study.setRecruitStatus(recruitStatusPolicy.resolve(
                study.getActYear(), study.getActSemester(), LocalDateTime.now()));
        // 멘토 계정을 따로 만들지 않는다. 멘토 권한은 tb_study의 멘토 관계에서
        // 요청 시점에 유도되므로, 승인된 순간부터 부원 로그인으로 관리할 수 있다.
    }

    /**
     * [어드민 전용] 현재 활동 학기의 자율스터디를 승인 상태로 생성한다.
     * 자율스터디도 일반 스터디와 같은 엔티티와 수강 신청 흐름을 사용하지만,
     * 멘토 개설 신청 및 심사 과정은 거치지 않으며, 개설한 운영진이 대표 멘토가 된다.
     */
    @Transactional
    public void createAutonomousStudy(Long adminUserId) {
        // 활성 학기 행을 잠가 여러 운영진이 동시에 요청해도 존재 확인과 저장이 직렬화된다.
        SemesterInfo semester = semesterService.getActiveForUpdate();
        // 정규 스터디와 같은 멘티 모집 흐름을 쓰므로, 모집이 끝난 뒤에는 빈 스터디 생성을 막는다.
        // 모집 시작 전 개설은 허용해 일정이 열리는 즉시 신청을 받을 수 있다.
        semesterPhaseGuard.requireNotEnded(
                SemesterPhase.MENTEE_RECRUIT, semester.actYear(), semester.actSemester());

        if (studyRepository.existsByActYearAndActSemesterAndAutonomousFlagTrue(
                semester.actYear(), semester.actSemester())) {
            throw new ForifException(ErrorCode.AUTONOMOUS_STUDY_ALREADY_EXISTS);
        }

        User admin = userRepository.findUserById(adminUserId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));
        Study study = Study.createAutonomousStudy(
                admin,
                semester.actYear(),
                semester.actSemester()
        );
        study.setRecruitStatus(recruitStatusPolicy.resolve(
                semester.actYear(),
                semester.actSemester(),
                LocalDateTime.now()
        ));
        studyRepository.saveStudy(study);
    }

    private void requireRegularStudyName(String studyName) {
        if (Study.isAutonomousStudyName(studyName)) {
            throw new ForifException(ErrorCode.AUTONOMOUS_STUDY_NAME_RESERVED);
        }
    }

    /**
     * [어드민 전용] 스터디 개설 거절
     */
    @Transactional
    public void rejectStudy(Integer studyId, String reason) {
        semesterPhaseGuard.requireOpen(SemesterPhase.MENTOR_REVIEW);

        Study study = studyRepository.findStudyById(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));
        requireActiveSemesterStudy(study);

        study.reject(reason); // 도메인 메서드 호출 (상태 변경 및 사유 저장)
    }

    /** 승인·반려는 현재 활동 학기의 개설 신청서에만 할 수 있다. */
    private void requireActiveSemesterStudy(Study study) {
        SemesterInfo active = semesterService.getActive();
        if (!active.matches(study.getActYear(), study.getActSemester())) {
            throw new ForifException(ErrorCode.STUDY_NOT_IN_ACTIVE_SEMESTER);
        }
    }
}
