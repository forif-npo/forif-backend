package org.forif_backend.application.study;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.forif_backend.application.file.dto.FileInfo;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.study.dto.CreateStudyApplyInfo;
import org.forif_backend.application.study.dto.StudyDto;
import org.forif_backend.application.study.dto.StudyInfo;
import org.forif_backend.application.study.dto.SemesterStudiesInfo;
import org.forif_backend.application.study.dto.UserStudiesResult;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.common.util.DateUtils;
import org.forif_backend.domain.study.*;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.web.study.dto.CreateStudyApplyRequest;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyService {

    private final StudyRepository studyRepository;
    private final MentorStudyRepository mentorStudyRepository;
    private final UserRepository userRepository;
    private final FilePort filePort;

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

        return mentorStudyRepository.findStudiesWithTagsByMentorId(mentorId)
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
    public CursorPageResponse<StudyDto> getAdminStudies(Integer cursor, int size, Integer year, Integer semester, String search) {
        List<Study> studies = studyRepository.searchStudiesWithCursor(cursor, size, year, semester, search);
        long totalElements = studyRepository.countStudies(year, semester, search);

        boolean hasNext = studies.size() > size;
        List<Study> content = hasNext ? studies.subList(0, size) : studies;

        List<StudyDto> dtos = content.stream().map(StudyDto::from).toList();
        Integer nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;

        return new CursorPageResponse<>(dtos, nextCursor, hasNext, totalElements);
    }

    /**
     * 스터디 개설 신청 저장 메서드입니다.
     * @param mentorId 개설 신청하는 유저 id
     * @param createStudyApplyRequest 신청 정보
     * @param thumbnail 썸네일 이미지 파일 정보
     * @param referenceFiles 참고 자료 파일 정보.
     * @return 클라이언트가 S3에 직접 파일을 업로드하는 데 사용할 Presigned URL 정보
     */
    @Transactional
    public CreateStudyApplyInfo createStudyApply(Long mentorId, CreateStudyApplyRequest createStudyApplyRequest, MultipartFile thumbnail, List<MultipartFile> referenceFiles) {
        // 유저 조회
        User mentor = userRepository.findUserById(mentorId).orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        // 스터디 태그 조회
        List<StudyTag> tags = studyRepository.findAllStudyTagById(createStudyApplyRequest.getStudyTagId());

        // 썸네일 업로드용 presigned url 생성 (optional)
        FileInfo thumbnailUploadInfo = null;
        String thumbnailKey = null;
        if (thumbnail != null) {
            thumbnailUploadInfo = filePort.generatePresignedUploadUrl(thumbnail);
            thumbnailKey = thumbnailUploadInfo.objectKey();
        }

        // 스터디 개설 내역 생성 (Study 사용)
        Study study = createStudyFromRequest(mentor, createStudyApplyRequest, tags, thumbnailKey);

        // 스터디 커리큘럼 생성
        List<StudyPlan> planList = createStudyApplyRequest.getStudyPlanList().stream()
                .map(plan -> createStudyPlan(plan, study)).toList();

        // 스터디 참고자료 생성
        List<FileInfo> referenceUploadInfos = new ArrayList<>();
        List<StudyReference> referenceList = createStudyApplyRequest.getReferences()
                .stream()
                .map(reference -> toReferenceEntity(reference, study, referenceFiles, referenceUploadInfos))
                .toList();

        // ---------- DB 저장 단계 ------------

        studyRepository.saveStudy(study);
        studyRepository.saveAllStudyPlan(planList);
        studyRepository.saveAllStudyReference(referenceList);

        // presigned url 반환
        return CreateStudyApplyInfo.builder()
                .thumbnailUploadInfo(thumbnailUploadInfo)
                .referenceUploadInfos(referenceUploadInfos)
                .build();
    }

    /**
     * CreateStudyApplyRequest로부터 Study 엔티티 생성
     */
    private Study createStudyFromRequest(User primaryMentor, CreateStudyApplyRequest request, List<StudyTag> tags, String thumbnailKey) {
        Study study = new Study();
        study.setPrimaryMentor(primaryMentor);
        study.setStudyName(request.getTitle());
        study.setSubTitle(request.getSubTitle());
        study.setTags(tags);
        study.setIsOnline(request.getIsOnline());
        study.setGoal(request.getGoal());
        study.setExplanation(request.getExplanation());
        study.setStartTime(request.getStartTime());
        study.setEndTime(request.getEndTime());
        study.setWeekDay(request.getWeekDay());
        study.setLocation(request.getStudyLocation());
        study.setLocationDetail(request.getStudyLocationDetail());
        study.setDifficulty(StudyDifficulty.fromLevel(request.getDifficulty()));
        study.setSelectionCriteria(request.getSelectionCriteria());
        study.setCapacity(request.getCapacity());
        study.setRequiresInterview(request.getRequiresInterview());
        study.setInterviewDate(request.getInterviewDate());
        study.setThumbnailImage(thumbnailKey);
        study.setIsApplied(true); // 신청 상태
        study.setActYear(DateUtils.getCurrentYear());
        study.setActSemester(DateUtils.getCurrentSemester());
        return study;
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
                    .orElseThrow(() -> new ForifException(ErrorCode.BAD_REQUEST, "파일 첨부가 잘못됐습니다."));

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
}
