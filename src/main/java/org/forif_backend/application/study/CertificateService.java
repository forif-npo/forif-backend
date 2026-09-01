package org.forif_backend.application.study;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.study.dto.CertificateTargetsResult;
import org.forif_backend.application.study.dto.IssueCertificatesResult;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.hackathon.HackathonRepository;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.staff.StaffRole;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyAttendanceRepository;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyUser;
import org.forif_backend.domain.study.StudyUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.forif_backend.application.file.FileViewUrls;
import org.forif_backend.application.file.TransactionalFileCleanup;

/**
 * 수료증 발급 서비스 (운영진 전용)
 * 발급 자격: 해당 스터디 출석 5회 이상 + 해당 학기 해커톤 참가 등록
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private static final String FILE_CLEANUP_CONTEXT = "회장 서명";
    private static final int REQUIRED_ATTENDANCE = 5;
    private static final DateTimeFormatter ISSUE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy. MM. dd.");

    private final StudyRepository studyRepository;
    private final StudyUserRepository studyUserRepository;
    private final StudyAttendanceRepository studyAttendanceRepository;
    private final HackathonRepository hackathonRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final CertificateImageGenerator certificateImageGenerator;
    private final FilePort filePort;

    /**
     * 현재 회장 계정 조회 (tb_staff_account의 affiliation='회장' ADMIN 계정)
     */
    private java.util.Optional<StaffAccount> findCurrentPresident() {
        return staffAccountRepository.findByAffiliation("회장").stream().findFirst();
    }

    /**
     * 현재 회장의 서명 이미지를 로드한다. 회장 계정이 없거나 서명 미등록이면 발급 불가.
     */
    private byte[] requirePresidentSignature(StaffAccount president) {
        if (president == null || president.getSignatureObjectKey() == null
                || president.getSignatureObjectKey().isBlank()) {
            throw new ForifException(ErrorCode.CERTIFICATE_SIGNATURE_NOT_FOUND);
        }
        return filePort.downloadBytes(president.getSignatureObjectKey());
    }

    /**
     * 서명 등록: 로그인한 운영진 본인의 계정에 서명 이미지를 저장한다.
     */
    @Transactional
    public String uploadSignature(Long userId, MultipartFile file) {
        StaffAccount account = staffAccountRepository.findByUserIdAndRole(userId, StaffRole.ADMIN)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

        String contentType = file != null ? file.getContentType() : null;
        if (file == null || file.isEmpty() || contentType == null || !contentType.startsWith("image/")) {
            throw new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT);
        }

        String previousObjectKey = account.getSignatureObjectKey();
        String objectKey = filePort.uploadFile(file, "signatures");
        account.updateSignature(objectKey);
        staffAccountRepository.save(account);

        // 교체된 서명은 커밋 후 지우고, 롤백되면 방금 올린 파일을 회수한다
        TransactionalFileCleanup.replaceAfterCompletion(
                filePort, singletonKey(previousObjectKey), singletonKey(objectKey), FILE_CLEANUP_CONTEXT);

        return filePort.generatePresignedViewUrl(objectKey).presignedUrl();
    }

    /**
     * 로그인한 운영진의 등록된 서명 URL 조회 (없으면 null)
     */
    @Transactional(readOnly = true)
    public String getSignatureUrl(Long userId) {
        StaffAccount account = staffAccountRepository.findByUserIdAndRole(userId, StaffRole.ADMIN)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

        String objectKey = account.getSignatureObjectKey();
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        return filePort.generatePresignedViewUrl(objectKey).presignedUrl();
    }

    /**
     * 발급 대상 조회: 스터디 멘티 전원의 출석 횟수, 해커톤 참여, 자격 여부, 발급 상태
     */
    @Transactional(readOnly = true)
    public CertificateTargetsResult getCertificateTargets(Integer studyId) {
        Study study = getStudy(studyId);

        List<StudyUser> mentees = studyUserRepository.findAllByStudyId(studyId);
        Map<Long, Long> presentCounts = studyAttendanceRepository.countPresentByStudyId(studyId);
        Set<Long> hackathonUserIds = new HashSet<>(hackathonRepository.findRegisteredUserIdsBySemester(
                study.getActYear(), study.getActSemester()));

        List<CertificateTargetsResult.Target> targets = mentees.stream()
                .map(mentee -> {
                    Long userId = mentee.getUser().getId();
                    long attendanceCount = presentCounts.getOrDefault(userId, 0L);
                    boolean hackathonParticipated = hackathonUserIds.contains(userId);
                    return CertificateTargetsResult.Target.builder()
                            .userId(userId)
                            .userName(mentee.getUser().getUserName())
                            .department(mentee.getUser().getDepartment())
                            .attendanceCount(attendanceCount)
                            .hackathonParticipated(hackathonParticipated)
                            .eligible(attendanceCount >= REQUIRED_ATTENDANCE && hackathonParticipated)
                            .certificateStatus(mentee.getCertificateStatus() != null
                                    ? mentee.getCertificateStatus() : 0)
                            .certificateUrl(resolveCertificateViewUrl(mentee.getCertificateObjectKey()))
                            .build();
                })
                .toList();

        return CertificateTargetsResult.builder()
                .studyId(study.getId())
                .studyName(study.getStudyName())
                .actYear(study.getActYear())
                .actSemester(study.getActSemester())
                .requiredAttendance(REQUIRED_ATTENDANCE)
                .targets(targets)
                .build();
    }

    /**
     * 수동 발급: 특수 케이스(자료 누락, 외부 과정, 과거 학기 재발행 등)를 위해
     * 모든 표기 정보를 직접 입력받아 수료증을 생성한다.
     * 자격 검증과 DB 기록 없이 이미지 생성·저장 후 URL만 반환한다.
     */
    public String issueManualCertificate(String userName, String studentNumber, String department,
                                         String studyName, String activityPeriod, String issueDate,
                                         String presidentName) {
        String normalizedStudyName = studyName == null ? null : studyName.trim();
        if (Study.isAutonomousStudyName(normalizedStudyName)) {
            throw new ForifException(ErrorCode.AUTONOMOUS_STUDY_OPERATION_NOT_ALLOWED);
        }

        String resolvedIssueDate = issueDate == null || issueDate.isBlank()
                ? LocalDate.now().format(ISSUE_DATE_FORMAT)
                : issueDate;

        // 회장 이름 미지정이거나 현재 회장과 같은 이름이면 현재 회장 서명을 합성하고,
        // 다른 이름(과거 학기 재발행)인 경우에만 서명 없이 생성한다
        StaffAccount president = findCurrentPresident().orElse(null);
        String currentPresidentName = president != null ? president.getName() : null;
        String requestedName = presidentName != null ? presidentName.trim() : "";

        String resolvedPresidentName;
        byte[] signature;
        if (requestedName.isEmpty() || requestedName.equals(currentPresidentName)) {
            signature = requirePresidentSignature(president);
            resolvedPresidentName = currentPresidentName;
        } else {
            resolvedPresidentName = requestedName;
            signature = null;
        }

        byte[] image = certificateImageGenerator.generate(
                userName, studentNumber, department, studyName, activityPeriod,
                resolvedIssueDate, resolvedPresidentName, signature);

        // 수동 발급분은 DB에 기록이 남지 않아 참조를 추적할 수 없다. 파일명에 타임스탬프를 붙이면
        // 재발급할 때마다 회수 불가능한 파일이 쌓이므로, 학번+스터디명으로 키를 정해 덮어쓴다.
        String filename = "%s-%s.png".formatted(studentNumber, manualCertificateSlug(studyName));
        String objectKey = filePort.uploadBytes(image, filename, "certificates/manual", "image/png");
        return filePort.generatePresignedViewUrl(objectKey).presignedUrl();
    }

    /**
     * 수료증 발급: 자격을 충족한 유저만 이미지 생성 → 파일 저장 → 발급 상태/URL 갱신.
     * 자격 미달 유저는 스킵되고 결과에 사유가 담긴다. 이미 발급된 유저는 재발급(덮어쓰기)된다.
     */
    @Transactional
    public IssueCertificatesResult issueCertificates(Integer studyId, List<Long> userIds, String activityPeriod,
                                                     boolean ignoreEligibility) {
        Study study = getStudy(studyId);

        Map<Long, StudyUser> menteeMap = studyUserRepository.findAllByStudyId(studyId).stream()
                .collect(Collectors.toMap(su -> su.getUser().getId(), su -> su));
        Map<Long, Long> presentCounts = studyAttendanceRepository.countPresentByStudyId(studyId);
        Set<Long> hackathonUserIds = new HashSet<>(hackathonRepository.findRegisteredUserIdsBySemester(
                study.getActYear(), study.getActSemester()));

        String issueDate = LocalDate.now().format(ISSUE_DATE_FORMAT);
        StaffAccount president = findCurrentPresident().orElse(null);
        byte[] signature = requirePresidentSignature(president);
        String presidentName = president.getName();
        String directory = "certificates/%d-%d/%d".formatted(
                study.getActYear(), study.getActSemester(), study.getId());

        List<IssueCertificatesResult.ItemResult> results = new ArrayList<>();
        int successCount = 0;

        for (Long userId : userIds) {
            StudyUser mentee = menteeMap.get(userId);
            if (mentee == null) {
                results.add(itemResult(userId, null, false, "스터디 멘티가 아닙니다.", null));
                continue;
            }

            String userName = mentee.getUser().getUserName();
            long attendanceCount = presentCounts.getOrDefault(userId, 0L);
            // 자격 미달자는 기본적으로 스킵하되, 운영진이 경고를 확인하고 강제 발급을 선택한 경우 허용
            if (!ignoreEligibility) {
                if (attendanceCount < REQUIRED_ATTENDANCE) {
                    results.add(itemResult(userId, userName, false,
                            "출석 횟수 미달 (%d/%d회)".formatted(attendanceCount, REQUIRED_ATTENDANCE), null));
                    continue;
                }
                if (!hackathonUserIds.contains(userId)) {
                    results.add(itemResult(userId, userName, false, "해당 학기 해커톤 미참여", null));
                    continue;
                }
            }

            byte[] image = certificateImageGenerator.generate(
                    userName,
                    String.valueOf(userId),
                    mentee.getUser().getDepartment(),
                    study.getStudyName(),
                    activityPeriod,
                    issueDate,
                    presidentName,
                    signature
            );

            String objectKey = filePort.uploadBytes(image, userId + ".png", directory, "image/png");
            String certificateUrl = filePort.generatePresignedViewUrl(objectKey).presignedUrl();

            mentee.issueCertificate(objectKey);
            studyUserRepository.save(mentee);

            results.add(itemResult(userId, userName, true, "발급 완료", certificateUrl));
            successCount++;
        }

        return IssueCertificatesResult.builder()
                .successCount(successCount)
                .skippedCount(results.size() - successCount)
                .results(results)
                .build();
    }

    private IssueCertificatesResult.ItemResult itemResult(Long userId, String userName, boolean success,
                                                          String message, String certificateUrl) {
        return IssueCertificatesResult.ItemResult.builder()
                .userId(userId)
                .userName(userName)
                .success(success)
                .message(message)
                .certificateUrl(certificateUrl)
                .build();
    }

    private String resolveCertificateViewUrl(String certificateObjectKey) {
        return FileViewUrls.resolveViewUrl(filePort, certificateObjectKey);
    }

    private static List<String> singletonKey(String objectKey) {
        return objectKey == null ? List.of() : List.of(objectKey);
    }

    /** 파일명에 쓸 수 있도록 스터디명을 정규화한다. 값이 없으면 고정 문자열을 쓴다. */
    private static String manualCertificateSlug(String studyName) {
        if (studyName == null || studyName.isBlank()) {
            return "manual";
        }
        String slug = studyName.trim().replaceAll("[^\\p{L}\\p{N}]+", "-");
        slug = slug.replaceAll("(^-+)|(-+$)", "");
        if (slug.isEmpty()) {
            return "manual";
        }
        return slug.length() > 50 ? slug.substring(0, 50) : slug;
    }

    private Study getStudy(Integer studyId) {
        Study study = studyRepository.findStudyById(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));
        if (study.isAutonomousStudy()) {
            throw new ForifException(ErrorCode.AUTONOMOUS_STUDY_OPERATION_NOT_ALLOWED);
        }
        return study;
    }
}
