package org.forif_backend.application.study;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.study.dto.IssueMentorConfirmationsResult;
import org.forif_backend.application.study.dto.MentorConfirmationStatusResult;
import org.forif_backend.application.study.dto.MentorConfirmationTargetsResult;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.study.MentorConfirmation;
import org.forif_backend.domain.study.MentorConfirmationRepository;
import org.forif_backend.domain.study.Study;
import org.forif_backend.domain.study.StudyRepository;
import org.forif_backend.domain.study.StudyStatus;
import org.forif_backend.domain.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MentorConfirmationService {

    private static final DateTimeFormatter ISSUE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy. MM. dd.");
    private static final DateTimeFormatter ACTIVITY_DATE_FORMAT = DateTimeFormatter
            .ofPattern("uuuu.MM.dd.")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern ACTIVITY_PERIOD_PATTERN = Pattern.compile(
            "^(\\d{4}\\.\\d{2}\\.\\d{2}\\.)~(\\d{4}\\.\\d{2}\\.\\d{2}\\.)$");

    private final StudyRepository studyRepository;
    private final MentorConfirmationRepository mentorConfirmationRepository;
    private final SemesterService semesterService;
    private final StaffAccountRepository staffAccountRepository;
    private final CertificateImageGenerator certificateImageGenerator;
    private final FilePort filePort;

    @Transactional(readOnly = true)
    public MentorConfirmationTargetsResult getTargets(Integer studyId) {
        Study study = getCompletedApprovedStudy(studyId);
        Map<Long, MentorConfirmation> confirmations = confirmationByMentorId(studyId);

        List<MentorConfirmationTargetsResult.Target> targets = mentorsOf(study).stream()
                .map(mentor -> {
                    MentorConfirmation confirmation = confirmations.get(mentor.getId());
                    return new MentorConfirmationTargetsResult.Target(
                            mentor.getId(),
                            mentor.getUserName(),
                            mentor.getDepartment(),
                            confirmation == null ? 0 : 1
                    );
                })
                .toList();

        return new MentorConfirmationTargetsResult(
                study.getId(), study.getStudyName(), study.getActYear(), study.getActSemester(), targets
        );
    }

    @Transactional
    public IssueMentorConfirmationsResult issueConfirmations(Integer studyId, List<Long> userIds,
                                                              String activityPeriod) {
        Study study = getCompletedApprovedStudy(studyId);
        validateActivityPeriod(activityPeriod);
        Map<Long, User> mentors = mentorsOf(study).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, mentor -> mentor));
        Map<Long, MentorConfirmation> confirmations = confirmationByMentorId(studyId);

        StaffAccount president = staffAccountRepository.findByAffiliation("회장").stream()
                .findFirst()
                .orElseThrow(() -> new ForifException(ErrorCode.CERTIFICATE_SIGNATURE_NOT_FOUND));
        if (president.getSignatureObjectKey() == null || president.getSignatureObjectKey().isBlank()) {
            throw new ForifException(ErrorCode.CERTIFICATE_SIGNATURE_NOT_FOUND);
        }
        byte[] signature = filePort.downloadBytes(president.getSignatureObjectKey());
        String issueDate = LocalDate.now().format(ISSUE_DATE_FORMAT);
        String directory = "mentor-confirmations/%d-%d/%d".formatted(
                study.getActYear(), study.getActSemester(), study.getId());

        List<IssueMentorConfirmationsResult.ItemResult> results = new ArrayList<>();
        int successCount = 0;
        for (Long userId : new LinkedHashSet<>(userIds)) {
            User mentor = mentors.get(userId);
            if (mentor == null) {
                results.add(new IssueMentorConfirmationsResult.ItemResult(
                        userId, null, false, "해당 스터디의 멘토가 아닙니다.", null));
                continue;
            }

            byte[] image = certificateImageGenerator.generateMentorConfirmation(
                    mentor.getUserName(),
                    String.valueOf(mentor.getId()),
                    mentor.getDepartment(),
                    study.getStudyName(),
                    activityPeriod,
                    issueDate,
                    president.getName(),
                    signature
            );
            String objectKey = filePort.uploadBytes(image, mentor.getId() + ".png", directory, "image/png");
            MentorConfirmation confirmation = confirmations.get(mentor.getId());
            if (confirmation == null) {
                confirmation = MentorConfirmation.issue(study, mentor, objectKey);
            } else {
                confirmation.reissue(objectKey);
            }
            mentorConfirmationRepository.save(confirmation);

            results.add(new IssueMentorConfirmationsResult.ItemResult(
                    mentor.getId(), mentor.getUserName(), true, "발급 완료", viewUrl(objectKey)));
            successCount++;
        }

        return new IssueMentorConfirmationsResult(successCount, results.size() - successCount, results);
    }

    @Transactional(readOnly = true)
    public MentorConfirmationStatusResult getMyConfirmation(Integer studyId, Long userId) {
        Study study = getCompletedApprovedStudy(studyId);
        requireStudyMentor(study, userId);

        return getConfirmationStatus(studyId, userId);
    }

    @Transactional(readOnly = true)
    public MentorConfirmationStatusResult getConfirmationForAdmin(Integer studyId, Long userId) {
        Study study = getCompletedApprovedStudy(studyId);
        requireStudyMentor(study, userId);
        return getConfirmationStatus(studyId, userId);
    }

    private MentorConfirmationStatusResult getConfirmationStatus(Integer studyId, Long userId) {
        Optional<MentorConfirmation> confirmation = mentorConfirmationRepository
                .findByStudyIdAndMentorId(studyId, userId);
        return confirmation
                .map(value -> new MentorConfirmationStatusResult(true, viewUrl(value.getConfirmationObjectKey())))
                .orElseGet(() -> new MentorConfirmationStatusResult(false, null));
    }

    private void requireStudyMentor(Study study, Long userId) {
        if (!study.isMentor(userId)) {
            throw new ForifException(ErrorCode.NOT_STUDY_MENTOR);
        }
    }

    private void validateActivityPeriod(String activityPeriod) {
        Matcher matcher = ACTIVITY_PERIOD_PATTERN.matcher(activityPeriod);
        if (!matcher.matches()) {
            throw new ForifException(ErrorCode.MENTOR_CONFIRMATION_INVALID_ACTIVITY_PERIOD);
        }

        try {
            LocalDate startDate = LocalDate.parse(matcher.group(1), ACTIVITY_DATE_FORMAT);
            LocalDate endDate = LocalDate.parse(matcher.group(2), ACTIVITY_DATE_FORMAT);
            if (startDate.isAfter(endDate)) {
                throw new ForifException(ErrorCode.MENTOR_CONFIRMATION_INVALID_ACTIVITY_PERIOD);
            }
        } catch (DateTimeParseException exception) {
            throw new ForifException(ErrorCode.MENTOR_CONFIRMATION_INVALID_ACTIVITY_PERIOD);
        }
    }

    private Study getCompletedApprovedStudy(Integer studyId) {
        Study study = studyRepository.findStudyById(studyId)
                .orElseThrow(() -> new ForifException(ErrorCode.STUDY_NOT_FOUND));
        SemesterInfo active = semesterService.getActive();
        boolean isCompletedSemester = study.getActYear() < active.actYear()
                || (study.getActYear() == active.actYear()
                && study.getActSemester() < active.actSemester());
        if (study.getStudyStatus() != StudyStatus.APPROVED || !isCompletedSemester) {
            throw new ForifException(ErrorCode.MENTOR_CONFIRMATION_NOT_AVAILABLE);
        }
        return study;
    }

    private List<User> mentorsOf(Study study) {
        Map<Long, User> mentors = new LinkedHashMap<>();
        addMentor(mentors, study.getPrimaryMentor());
        addMentor(mentors, study.getSecondaryMentor());
        return List.copyOf(mentors.values());
    }

    private void addMentor(Map<Long, User> mentors, User mentor) {
        if (mentor != null && mentor.getId() != null) {
            mentors.putIfAbsent(mentor.getId(), mentor);
        }
    }

    private Map<Long, MentorConfirmation> confirmationByMentorId(Integer studyId) {
        return mentorConfirmationRepository.findAllByStudyId(studyId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        confirmation -> confirmation.getMentor().getId(),
                        confirmation -> confirmation,
                        (first, ignored) -> first
                ));
    }

    private String viewUrl(String objectKey) {
        return filePort.generatePresignedViewUrl(objectKey).presignedUrl();
    }
}
