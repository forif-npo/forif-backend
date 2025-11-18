package org.forif_backend.application.studyApply.dto;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.file.dto.FileInfo;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.study.*;
import org.forif_backend.domain.studyApply.*;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.web.studyApply.dto.CreateStudyApplyRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StudyApplyService {
    private final UserRepository userRepository;
    private final StudyApplyRepository studyApplyRepository;
    private final StudyRepository studyRepository;
    private final FilePort filePort;

    /**
     * 스터디 개설 신청 저장 메서드입니다.
     * @param MentorId 개설 신청하는 유저 id
     * @param createStudyApplyRequest 신청 정보
     * @param thumbnail 썸네일 이미지 파일 정보
     * @param referenceFiles 참고 자료 파일 정보.
     * @return 클라이언트가 S3에 직접 파일을 업로드하는 데 사용할 Presigned URL 정보
     */
    @Transactional
    public CreateStudyApplyInfo createStudyApply(Long MentorId, CreateStudyApplyRequest createStudyApplyRequest, MultipartFile thumbnail, List<MultipartFile> referenceFiles) {
        // 유저 조회
        User mentor = userRepository.findUserById(MentorId).orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        // 스터디 태그 조회
        List<StudyTag> tags = studyRepository.findAllStudyTagById(createStudyApplyRequest.studyTagId());

        // 썸네일 업로드용 presigned url 생성
        FileInfo thumbnailUploadInfo = filePort.generatePresignedUploadUrl(thumbnail);

        // 스터디 개설 내역 생성
        StudyApply studyApply = StudyApply.create(mentor, createStudyApplyRequest, tags, thumbnailUploadInfo.objectKey());

        // 스터디 커리큘럼 생성
        List<StudyApplyPlan> planList = createStudyApplyRequest.studyPlanList().stream()
                .map(plan -> StudyApplyPlan.create(plan, studyApply)).toList();

        // 스터디 참고자료 생성
        List<FileInfo> referenceUploadInfos = new ArrayList<>();
        List<StudyApplyReference> referenceList = createStudyApplyRequest.references()
                .stream()
                .map(reference -> toReferenceEntity(reference, studyApply, referenceFiles, referenceUploadInfos))
                .toList();

        // ---------- DB 저장 단계 ------------

        studyApplyRepository.saveStudyApply(studyApply);
        studyApplyRepository.saveAllStudyApplyPlan(planList);
        studyApplyRepository.saveAllStudyApplyReference(referenceList);

        // presigned url 반환
        return CreateStudyApplyInfo.builder()
                .thumbnailUploadInfo(thumbnailUploadInfo)
                .referenceUploadInfos(referenceUploadInfos)
                .build();
    }

    /**
     * Reference DTO를 StudyApplyReference 엔티티로 변환하고,
     * 파일 타입일 경우 Presigned URL 정보를 'referenceUploadInfos' 리스트에 추가합니다.
     * @param reference 참고 자료 정보
     * @param studyApply 스터디 개설 정보
     * @param referenceFiles 참고 자료 파일 정보들
     * @param referenceUploadInfos 클라이언트에 반환할 presigned url list
     * @return
     */
    private StudyApplyReference toReferenceEntity(CreateStudyApplyRequest.Reference reference, StudyApply studyApply, List<MultipartFile> referenceFiles, List<FileInfo> referenceUploadInfos) {
        String content;
        if (reference.type() == ReferenceType.FILE) { // 참고자료 타입이 파일인 경우
            // 이름에 맞는 파일 찾기
            MultipartFile file = referenceFiles.stream().filter(referenceFile -> Objects.equals(referenceFile.getOriginalFilename(), reference.fileName())).findAny()
                    .orElseThrow(() -> new ForifException(ErrorCode.BAD_REQUEST, "파일 첨부가 잘못됐습니다."));

            // 업로드용 presigned url 생성
            FileInfo fileInfo = filePort.generatePresignedUploadUrl(file);

            // 반환값에 추가
            referenceUploadInfos.add(fileInfo);

            content = fileInfo.objectKey();
        } else { // 참고자료 타입이 url인 경우
            content = reference.url();
        }
        return StudyApplyReference.create(studyApply, reference.type(), content);
    }
}
