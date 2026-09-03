package org.forif_backend.application.notification;

import org.forif_backend.application.notification.dto.SendAlimTalkCommand;
import org.forif_backend.application.notification.dto.SendAlimTalkMessageResult;
import org.forif_backend.application.notification.dto.SendAlimTalkResult;
import org.forif_backend.application.notification.port.out.NotificationSendPort;
import org.forif_backend.application.notification.dto.NotificationRecipientTarget;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.user.UserService;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffAccountRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.application.user.dto.MemberInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceRecipientTest {

    private static final CursorPageResponse<MemberInfo> EMPTY_PAGE =
            CursorPageResponse.ofCursor(java.util.List.of(), null, false, 0);

    @Mock
    private NotificationSendPort notificationSendPort;
    @Mock
    private StaffAccountRepository staffAccountRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SemesterService semesterService;
    @Mock
    private UserService userService;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        lenient().when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 1));
    }

    @Test
    void sendsResolvableRecipientsAndReturnsLookupFailureForUnresolvableRecipient() {
        String validReceiver = "01011112222";
        String missingReceiver = "01033334444";
        String failedReceiver = "01055556666";
        stubAuthorizedSender();
        stubUser(validReceiver, "김포리");
        when(userRepository.findByPhoneNum(missingReceiver)).thenReturn(Optional.empty());
        stubUser(failedReceiver, "이포리");
        when(notificationSendPort.sendAlimTalk(any(), any())).thenReturn(CompletableFuture.completedFuture(
                new SendAlimTalkResult("template-1", List.of(
                        new SendAlimTalkMessageResult(validReceiver, true, null, null),
                        new SendAlimTalkMessageResult(failedReceiver, false, "SOLAPI-400", "수신 거부")
                ))
        ));

        SendAlimTalkResult result = notificationService.sendAlimTalk(
                new SendAlimTalkCommand(
                        List.of(validReceiver, missingReceiver, failedReceiver),
                        "template-1",
                        Map.of("#{내용}", "안내")
                ),
                1L
        ).join();

        ArgumentCaptor<SendAlimTalkCommand> commandCaptor = ArgumentCaptor.forClass(SendAlimTalkCommand.class);
        verify(notificationSendPort).sendAlimTalk(
                commandCaptor.capture(),
                eq(Map.of(validReceiver, "김포리", failedReceiver, "이포리"))
        );
        assertThat(commandCaptor.getValue().receivers()).containsExactly(validReceiver, failedReceiver);
        assertThat(result.templateId()).isEqualTo("template-1");
        assertThat(result.results()).containsExactly(
                new SendAlimTalkMessageResult(validReceiver, true, null, null),
                new SendAlimTalkMessageResult(
                        missingReceiver,
                        false,
                        ErrorCode.USER_NOT_FOUND.getCode(),
                        ErrorCode.USER_NOT_FOUND.getMessage()
                ),
                new SendAlimTalkMessageResult(failedReceiver, false, "SOLAPI-400", "수신 거부")
        );
    }

    @Test
    void doesNotCallProviderWhenEveryRecipientIsUnresolvable() {
        String firstMissingReceiver = "01033334444";
        String secondMissingReceiver = "01055556666";
        stubAuthorizedSender();
        when(userRepository.findByPhoneNum(firstMissingReceiver)).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNum(secondMissingReceiver)).thenReturn(Optional.empty());

        SendAlimTalkResult result = notificationService.sendAlimTalk(
                new SendAlimTalkCommand(
                        List.of(firstMissingReceiver, secondMissingReceiver),
                        "template-1",
                        null
                ),
                1L
        ).join();

        verifyNoInteractions(notificationSendPort);
        assertThat(result.templateId()).isEqualTo("template-1");
        assertThat(result.results()).containsExactly(
                new SendAlimTalkMessageResult(
                        firstMissingReceiver,
                        false,
                        ErrorCode.USER_NOT_FOUND.getCode(),
                        ErrorCode.USER_NOT_FOUND.getMessage()
                ),
                new SendAlimTalkMessageResult(
                        secondMissingReceiver,
                        false,
                        ErrorCode.USER_NOT_FOUND.getCode(),
                        ErrorCode.USER_NOT_FOUND.getMessage()
                )
        );
    }

    @Test
    void sendsDuplicateResolvableRecipientOnlyOnce() {
        String receiver = "01011112222";
        stubAuthorizedSender();
        stubUser(receiver, "김포리");
        when(notificationSendPort.sendAlimTalk(any(), any())).thenReturn(CompletableFuture.completedFuture(
                new SendAlimTalkResult("template-1", List.of(
                        new SendAlimTalkMessageResult(receiver, true, null, null)
                ))
        ));

        SendAlimTalkResult result = notificationService.sendAlimTalk(
                new SendAlimTalkCommand(List.of(receiver, receiver), "template-1", null),
                1L
        ).join();

        ArgumentCaptor<SendAlimTalkCommand> commandCaptor = ArgumentCaptor.forClass(SendAlimTalkCommand.class);
        verify(notificationSendPort).sendAlimTalk(commandCaptor.capture(), eq(Map.of(receiver, "김포리")));
        verify(userRepository, times(1)).findByPhoneNum(receiver);
        assertThat(commandCaptor.getValue().receivers()).containsExactly(receiver);
        assertThat(result.results()).containsExactly(new SendAlimTalkMessageResult(receiver, true, null, null));
    }

    @Test
    void returnsDuplicateUnresolvableRecipientOnlyOnceWithoutSending() {
        String receiver = "01033334444";
        stubAuthorizedSender();
        when(userRepository.findByPhoneNum(receiver)).thenReturn(Optional.empty());

        SendAlimTalkResult result = notificationService.sendAlimTalk(
                new SendAlimTalkCommand(List.of(receiver, receiver), "template-1", null),
                1L
        ).join();

        verify(userRepository, times(1)).findByPhoneNum(receiver);
        verifyNoInteractions(notificationSendPort);
        assertThat(result.results()).containsExactly(new SendAlimTalkMessageResult(
                receiver,
                false,
                ErrorCode.USER_NOT_FOUND.getCode(),
                ErrorCode.USER_NOT_FOUND.getMessage()
        ));
    }

    private void stubAuthorizedSender() {
        when(staffAccountRepository.findByUserId(1L)).thenReturn(Optional.of(mock(StaffAccount.class)));
    }

    private void stubUser(String phoneNumber, String userName) {
        User user = mock(User.class);
        when(user.getUserName()).thenReturn(userName);
        when(userRepository.findByPhoneNum(phoneNumber)).thenReturn(Optional.of(user));
    }

    @Test
    void getsCurrentSemesterMembers() {
        when(userService.getNotificationMembers(2026, 1, null, 100, "김"))
                .thenReturn(EMPTY_PAGE);

        CursorPageResponse<MemberInfo> result = notificationService.getRecipients(
                NotificationRecipientTarget.CURRENT_SEMESTER_MEMBERS, null, 100, "김");

        assertThat(result).isSameAs(EMPTY_PAGE);
    }

    @Test
    void getsCurrentSemesterApplicantsRegardlessOfAcceptanceStatus() {
        when(userService.getApplicants(2026, 1, null, 100, "김"))
                .thenReturn(EMPTY_PAGE);

        CursorPageResponse<MemberInfo> result = notificationService.getRecipients(
                NotificationRecipientTarget.CURRENT_SEMESTER_APPLICANTS, null, 100, "김");

        assertThat(result).isSameAs(EMPTY_PAGE);
    }

    @Test
    void getsPreviousSemesterMembersAcrossYearBoundary() {
        when(userService.getNotificationMembers(2025, 2, null, 100, "김"))
                .thenReturn(EMPTY_PAGE);

        CursorPageResponse<MemberInfo> result = notificationService.getRecipients(
                NotificationRecipientTarget.PREVIOUS_SEMESTER_MEMBERS, null, 100, "김");

        assertThat(result).isSameAs(EMPTY_PAGE);
        verify(userService).getNotificationMembers(eq(2025), eq(2), isNull(), eq(100), eq("김"));
    }

    @Test
    void getsAllMembers() {
        when(userService.getNotificationMembers(null, 100, "김"))
                .thenReturn(EMPTY_PAGE);

        CursorPageResponse<MemberInfo> result = notificationService.getRecipients(
                NotificationRecipientTarget.ALL_MEMBERS, null, 100, "김");

        assertThat(result).isSameAs(EMPTY_PAGE);
    }

    @Test
    void getsAcceptedUsersWhoHaveNotPaidDues() {
        when(userService.getAcceptedUsersMissingDues(2026, 1, null, 100, "김"))
                .thenReturn(EMPTY_PAGE);

        CursorPageResponse<MemberInfo> result = notificationService.getRecipients(
                NotificationRecipientTarget.ACCEPTED_DUES_UNPAID, null, 100, "김");

        assertThat(result).isSameAs(EMPTY_PAGE);
    }

    @Test
    void getsAcceptedUsersWhoHaveNotSubmittedGoogleForm() {
        when(userService.getAcceptedUsersMissingGoogleForm(2026, 1, null, 100, "김"))
                .thenReturn(EMPTY_PAGE);

        CursorPageResponse<MemberInfo> result = notificationService.getRecipients(
                NotificationRecipientTarget.ACCEPTED_GOOGLE_FORM_NOT_SUBMITTED, null, 100, "김");

        assertThat(result).isSameAs(EMPTY_PAGE);
    }
}
