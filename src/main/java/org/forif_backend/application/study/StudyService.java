package org.forif_backend.application.study;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.file.dto.FileInfo;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.staff.dto.CreateMentorCommand;
import org.forif_backend.application.study.dto.*;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.common.util.DateUtils;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.study.*;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.web.study.dto.CreateStudyApplyRequest;
import org.forif_backend.web.study.dto.UpdateStudyRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyService {

    // TODO: 하드코딩된 기본 비밀번호 개선 필요. 멘토가 직접 초기 비밀번호를 설정하거나,
    //       랜덤 생성 후 이메일 발송하는 방식으로 변경 필요.
    private static final String DEFAULT_MENTOR_PASSWORD = "forif1234";

    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final FilePort filePort;
    private final StaffAccountService staffAccountService;
    private final StaffAccountRepository staffAccountRepository;

    @Transactional(readOnly = true)
    public List<StudyDto> getStudies(Long page, Long pageSize, Integer year, Integer semester,
                                     List<StudyDifficulty> difficulties, List<String> tags,
                                     RecruitStatus recruitStatus, String search) {
        
        // Build search condition
        StudySearchCond searchCond = StudySearchCond.builder()
            .year(year)
            .semester(semester)
            .difficulties(difficulties)
            .studyTagNames(tags)
            .recruitStatus(recruitStatus)
            .searchKeyword(search)
            .build();
        
        // Get studies from repository
        List<Study> studies = studyRepository.getStudies(searchCond, page, pageSize);

        return studies.stream().map(StudyDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<StudyDto> getMyCreatedStudies(Long mentorId) {

        return studyRepository.findStudiesByMentorId(mentorId)
            .stream()
            .map(StudyDto::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public UserStudiesResult getUserStudies(Long userId) {
        // 1. userId로 스터디 목록 조회 (이미 연도, 학기 내림차순으로 정렬됨)
        List<Study> studies = studyRepository.findStudiesByUserId(userId);

        // 2. 현재 학기 정보
        int currentYear = DateUtils.getCurrentYear();
        int currentSemester = DateUtils.getCurrentSemester();

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
                                            .study(StudyInfo.from(firstStudy))  // 첫 번째 스터디만 (한 학기에 1개)
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

        return StudyDetailDto.of(study, plans, references, mentorStudies);
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<AdminStudyDto> getAdminStudies(Integer cursor, int size, Integer year, Integer semester, String search) {
        List<Study> studies = studyRepository.searchStudiesWithCursor(cursor, size, year, semester, search);
        long totalElements = studyRepository.countStudies(year, semester, search);

        boolean hasNext = studies.size() > size;
        List<Study> content = hasNext ? studies.subList(0, size) : studies;

        List<Integer> studyIds = content.stream().map(Study::getId).toList();
        Map<Integer, Long> menteeCountMap = studyRepository.countMenteesByStudyIds(studyIds);

        List<AdminStudyDto> dtos = content.stream()
                .map(s -> AdminStudyDto.of(s, menteeCountMap.getOrDefault(s.getId(), 0L)))
                .toList();
        Integer nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;

        return new CursorPageResponse<>(dtos, nextCursor, hasNext, totalElements);
    }

    @Transactional
    public void updateStudy(Integer studyId, UpdateStudyRequest request) {
        Study study = studyRepository.findStudyByIdWithTags(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        // null이 아닌 기본 필드만 반영
        if (request.getStudyName() != null) study.setStudyName(request.getStudyName());
        if (request.getSubTitle() != null) study.setSubTitle(request.getSubTitle());
        if (request.getOneLiner() != null) study.setOneLiner(request.getOneLiner());
        if (request.getExplanation() != null) study.setExplanation(request.getExplanation());
        if (request.getGoal() != null) study.setGoal(request.getGoal());
        if (request.getStartTime() != null) study.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) study.setEndTime(request.getEndTime());
        if (request.getWeekDay() != null) study.setWeekDay(request.getWeekDay());
        if (request.getLocation() != null) study.setLocation(request.getLocation());
        if (request.getLocationDetail() != null) study.setLocationDetail(request.getLocationDetail());
        if (request.getIsOnline() != null) study.setIsOnline(request.getIsOnline());
        if (request.getCapacity() != null) study.setCapacity(request.getCapacity());
        if (request.getSelectionCriteria() != null) study.setSelectionCriteria(request.getSelectionCriteria());
        if (request.getRequiresInterview() != null) study.setRequiresInterview(request.getRequiresInterview());
        if (request.getInterviewDate() != null) study.setInterviewDate(request.getInterviewDate());

        // enum 변환 필드
        if (request.getDifficulty() != null) {
            study.setDifficulty(StudyDifficulty.fromLevel(request.getDifficulty()));
        }
        if (request.getRecruitStatus() != null) {
            study.setRecruitStatus(RecruitStatus.fromValue(request.getRecruitStatus()));
        }

        // 태그 교체
        if (request.getStudyTagIds() != null) {
            List<StudyTag> tags = studyRepository.findAllStudyTagById(request.getStudyTagIds());
            study.setTags(tags);
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

        // 참고자료: 기존 삭제 후 재생성
        if (request.getReferences() != null) {
            studyRepository.deleteStudyReferencesByStudyId(studyId);
            List<StudyReference> references = request.getReferences().stream()
                    .map(ref -> StudyReference.create(study, ref.getType(), ref.getUrl()))
                    .toList();
            studyRepository.saveAllStudyReference(references);
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
        studyRepository.deleteStudyById(studyId);
    }

    /**
     * 스터디 개설 신청 저장 메서드입니다.
     * @param mentorId 개설 신청하는 유저 id
     * @param request 신청 정보
     * @param thumbnail 썸네일 이미지 파일 정보
     * @param referenceFiles 참고 자료 파일 정보.
     * @return 클라이언트가 S3에 직접 파일을 업로드하는 데 사용할 Presigned URL 정보
     */
    @Transactional
    public CreateStudyApplyInfo createStudyApply(Long mentorId, CreateStudyApplyRequest request,
                                                 MultipartFile thumbnail, List<MultipartFile> referenceFiles) {
        User mentor = userRepository.findUserById(mentorId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        // 생성 시점에 필요한 최소한의 정보만 주입
        Study study = Study.createPendingStudy(mentor);

        List<StudyTag> tags = studyRepository.findAllStudyTagById(request.getStudyTagId());

        // 공통 데이터 반영
        study.applyRequestData(request, tags);

        return saveStudyWithResources(study, request, thumbnail, referenceFiles);
    }

    /**
     * StudyPlan 생성
     */
    private StudyPlan createStudyPlan(CreateStudyApplyRequest.Plan planRequest, Study study) {
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
     * 파일 타입일 경우 Presigned URL 정보를 'referenceUploadInfos' 리스트에 추가합니다.
     */
    private StudyReference toReferenceEntity(CreateStudyApplyRequest.Reference reference, Study study, List<MultipartFile> referenceFiles, List<FileInfo> referenceUploadInfos) {
        String content;
        // ReferenceType을 study 패키지의 것으로 변환
        ReferenceType refType = ReferenceType.valueOf(reference.getType().name());

        if (refType == ReferenceType.FILE) { // 참고자료 타입이 파일인 경우
            // 이름에 맞는 파일 찾기
            MultipartFile file = referenceFiles.stream().filter(referenceFile -> Objects.equals(referenceFile.getOriginalFilename(), reference.getFileName())).findAny()
                    .orElseThrow(() -> new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT));

            // 업로드용 presigned url 생성
            FileInfo fileInfo = filePort.generatePresignedUploadUrl(file);

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
     * @return S3 업로드를 위한 Presigned URL 정보가 담긴 Info 객체
     */
    @Transactional
    public CreateStudyApplyInfo reApplyStudy(Integer studyId, Long userId, CreateStudyApplyRequest request,
                                             MultipartFile thumbnail, List<MultipartFile> referenceFiles) {

        // 1. 스터디 조회 (태그 포함)
        Study study = studyRepository.findStudyByIdWithTags(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        // 2. 권한 검증 및 상태 변경
        if (!study.isMentor(userId)) {
            throw new ForifException(ErrorCode.INSUFFICIENT_PERMISSION);
        }
        study.reApply(); // 내부에서 상태값을 변경하는 로직

        // 3. 기본 데이터 업데이트 (스터디명, 설명, 태그 등)
        List<StudyTag> tags = studyRepository.findAllStudyTagById(request.getStudyTagId());
        study.applyRequestData(request, tags);

        // 4. 기존 연관 리소스(커리큘럼, 참고자료) 삭제
        // 재신청은 기존 내용을 덮어쓰는 개념이므로 삭제 후 재등록
        studyRepository.deleteStudyPlansByStudyId(studyId);
        studyRepository.deleteStudyReferencesByStudyId(studyId);

        // 5. 신규 리소스 저장 및 Presigned URL 생성
        // 기존에 만들어둔 공통 메서드를 호출하고 그 결과를 그대로 반환합니다.
        return saveStudyWithResources(study, request, thumbnail, referenceFiles);
    }

    /**
     * [공통] 스터디 리소스(파일, 플랜, 참고자료) 처리 및 DB 저장
     */
    private CreateStudyApplyInfo saveStudyWithResources(Study study, CreateStudyApplyRequest request,
                                                        MultipartFile thumbnail, List<MultipartFile> referenceFiles) {
        // 썸네일 처리
        FileInfo thumbnailInfo = null;
        if (thumbnail != null) {
            thumbnailInfo = filePort.generatePresignedUploadUrl(thumbnail);
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

        List<StudyReference> referenceList = Optional.ofNullable(request.getReferences())
                .orElseGet(Collections::emptyList) // null이면 빈 리스트 반환
                .stream()
                .map(ref -> toReferenceEntity(ref, study, referenceFiles, referenceUploadInfos))
                .toList();

        // DB 저장
        studyRepository.saveStudy(study);
        studyRepository.saveAllStudyPlan(planList);
        studyRepository.saveAllStudyReference(referenceList);

        return CreateStudyApplyInfo.builder()
                .thumbnailUploadInfo(thumbnailInfo)
                .referenceUploadInfos(referenceUploadInfos)
                .build();
    }

    /**
     * [어드민 전용] 스터디 개설 승인
     * 승인 시 멘토(primary, secondary) 계정이 없으면 자동 생성
     */
    @Transactional
    public void approveStudy(Integer studyId) {
        Study study = studyRepository.findStudyById(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        study.approve();

        // 멘토 계정 자동 생성
        createMentorAccountIfAbsent(study.getPrimaryMentor(), study.getStudyName());
        if (study.getSecondaryMentor() != null) {
            createMentorAccountIfAbsent(study.getSecondaryMentor(), study.getStudyName());
        }
    }

    /**
     * 멘토 계정이 없으면 기본 비밀번호로 자동 생성
     */
    private void createMentorAccountIfAbsent(User mentor, String studyName) {
        if (staffAccountRepository.existsById(mentor.getId())) {
            return;
        }

        CreateMentorCommand command = new CreateMentorCommand(
                mentor.getId(),
                DEFAULT_MENTOR_PASSWORD,
                studyName
        );
        staffAccountService.createMentorAccount(command);
    }

    /**
     * [어드민 전용] 스터디 개설 거절
     */
    @Transactional
    public void rejectStudy(Integer studyId, String reason) {
        Study study = studyRepository.findStudyById(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));

        study.reject(reason); // 도메인 메서드 호출 (상태 변경 및 사유 저장)
    }
}
