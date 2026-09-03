package org.forif_backend.application.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.notification.dto.SendAlimTalkCommand;
import org.forif_backend.application.notification.dto.SendAlimTalkMessageResult;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int MAX_RECIPIENT_PAGE_SIZE = 100;
    private static final String UNKNOWN_FAILURE_CODE = "UNKNOWN";
    private static final String MISSING_PROVIDER_RESULT_MESSAGE = "발송 결과를 확인할 수 없습니다.";

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

        List<String> uniqueReceivers = command.receivers().stream()
                .distinct()
                .toList();
        Map<String, String> receiverNames = new HashMap<>();
        Map<String, SendAlimTalkMessageResult> lookupFailuresByReceiver = new HashMap<>();
        List<String> validReceivers = new ArrayList<>();

        for (String receiver : uniqueReceivers) {
            resolveReceiver(receiver, receiverNames, lookupFailuresByReceiver);
            if (receiverNames.containsKey(receiver)) {
                validReceivers.add(receiver);
            }
        }

        if (validReceivers.isEmpty()) {
            return CompletableFuture.completedFuture(new SendAlimTalkResult(
                    command.templateCode(),
                    mergeResults(uniqueReceivers, List.of(), lookupFailuresByReceiver)
            ));
        }

        SendAlimTalkCommand validReceiverCommand = new SendAlimTalkCommand(
                validReceivers,
                command.templateCode(),
                command.variables()
        );

        return notificationSendPort.sendAlimTalk(validReceiverCommand, receiverNames)
                .thenApply(sentResult -> new SendAlimTalkResult(
                        sentResult.templateId(),
                        mergeResults(uniqueReceivers, sentResult.results(), lookupFailuresByReceiver)
                ));
    }

    private void resolveReceiver(
            String receiver,
            Map<String, String> receiverNames,
            Map<String, SendAlimTalkMessageResult> lookupFailuresByReceiver
    ) {
        if (receiverNames.containsKey(receiver) || lookupFailuresByReceiver.containsKey(receiver)) {
            return;
        }

        if (receiver == null) {
            lookupFailuresByReceiver.put(null, userNotFoundResult(null));
            return;
        }

        userRepository.findByPhoneNum(receiver)
                .map(User::getUserName)
                .ifPresentOrElse(
                        userName -> receiverNames.put(receiver, userName),
                        () -> lookupFailuresByReceiver.put(receiver, userNotFoundResult(receiver))
                );
    }

    private List<SendAlimTalkMessageResult> mergeResults(
            List<String> requestedReceivers,
            List<SendAlimTalkMessageResult> sentResults,
            Map<String, SendAlimTalkMessageResult> lookupFailuresByReceiver
    ) {
        List<SendAlimTalkMessageResult> mergedResults = new ArrayList<>();
        int sentResultIndex = 0;

        for (String receiver : requestedReceivers) {
            SendAlimTalkMessageResult lookupFailure = lookupFailuresByReceiver.get(receiver);
            if (lookupFailure != null) {
                mergedResults.add(lookupFailure);
                continue;
            }
            if (sentResultIndex < sentResults.size()) {
                mergedResults.add(sentResults.get(sentResultIndex++));
                continue;
            }
            mergedResults.add(new SendAlimTalkMessageResult(
                    receiver,
                    false,
                    UNKNOWN_FAILURE_CODE,
                    MISSING_PROVIDER_RESULT_MESSAGE
            ));
        }

        return mergedResults;
    }

    private SendAlimTalkMessageResult userNotFoundResult(String receiver) {
        return new SendAlimTalkMessageResult(
                receiver,
                false,
                ErrorCode.USER_NOT_FOUND.getCode(),
                ErrorCode.USER_NOT_FOUND.getMessage()
        );
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
