package org.forif_backend.application.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.notification.dto.SendAlimTalkCommand;
import org.forif_backend.application.notification.dto.SendAlimTalkResult;
import org.forif_backend.application.notification.dto.TemplateInfo;
import org.forif_backend.application.notification.dto.NotificationRecipientTarget;
import org.forif_backend.application.notification.port.out.NotificationSendPort;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.user.UserService;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.application.user.dto.MemberInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int MAX_RECIPIENT_PAGE_SIZE = 100;

    private final NotificationSendPort notificationSendPort;
    private final StaffAccountRepository staffAccountRepository;
    private final UserRepository userRepository;
    private final SemesterService semesterService;
    private final UserService userService;

    public CompletableFuture<SendAlimTalkResult> sendAlimTalk(
            SendAlimTalkCommand command,
            Long senderId)  {

        // 권한 확인
        staffAccountRepository.findByUserId(senderId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

        // 수신자별 이름 조회 (전화번호 -> 이름)
        Map<String, String> receiverNames = command.receivers().stream()
                .distinct()
                .collect(Collectors.toMap(
                        phoneNumber -> phoneNumber,
                        phoneNumber -> userRepository.findByPhoneNum(phoneNumber)
                                .map(User::getUserName)
                                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND))
                ));

        // 알림톡 전송 (수신자별 이름 포함)
        return notificationSendPort.sendAlimTalk(command, receiverNames);
    }

    public List<TemplateInfo> getKakaoTemplates(Long userId) {
        staffAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.STAFF_NOT_FOUND));

        return notificationSendPort.getKakaoTemplates();
    }

    public CursorPageResponse<MemberInfo> getRecipients(
            NotificationRecipientTarget target,
            Long cursor,
            int size,
            String search
    ) {
        int safeSize = Math.max(1, Math.min(size, MAX_RECIPIENT_PAGE_SIZE));
        SemesterInfo currentSemester = semesterService.getActive();

        return switch (target) {
            case CURRENT_SEMESTER_MEMBERS -> userService.getNotificationMembers(
                    currentSemester.actYear(), currentSemester.actSemester(), cursor, safeSize, search);
            case CURRENT_SEMESTER_APPLICANTS -> userService.getApplicants(
                    currentSemester.actYear(), currentSemester.actSemester(), cursor, safeSize, search);
            case CURRENT_SEMESTER_ACCEPTED_APPLICANTS -> userService.getAcceptedApplicants(
                    currentSemester.actYear(), currentSemester.actSemester(), cursor, safeSize, search);
            case CURRENT_SEMESTER_REJECTED_APPLICANTS -> userService.getRejectedApplicants(
                    currentSemester.actYear(), currentSemester.actSemester(), cursor, safeSize, search);
            case PREVIOUS_SEMESTER_MEMBERS -> {
                SemesterInfo previousSemester = previousOf(currentSemester);
                yield userService.getNotificationMembers(
                        previousSemester.actYear(), previousSemester.actSemester(), cursor, safeSize, search);
            }
            case ALL_MEMBERS -> userService.getNotificationMembers(cursor, safeSize, search);
            case ACCEPTED_DUES_UNPAID -> userService.getAcceptedUsersMissingDues(
                    currentSemester.actYear(), currentSemester.actSemester(), cursor, safeSize, search);
            case ACCEPTED_GOOGLE_FORM_NOT_SUBMITTED -> userService.getAcceptedUsersMissingGoogleForm(
                    currentSemester.actYear(), currentSemester.actSemester(), cursor, safeSize, search);
        };
    }

    private SemesterInfo previousOf(SemesterInfo semester) {
        return semester.actSemester() == 1
                ? SemesterInfo.of(semester.actYear() - 1, 2)
                : SemesterInfo.of(semester.actYear(), 1);
    }
}
