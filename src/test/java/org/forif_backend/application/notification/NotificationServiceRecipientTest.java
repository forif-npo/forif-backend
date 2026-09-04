package org.forif_backend.application.notification;

import org.forif_backend.application.notification.port.out.NotificationSendPort;
import org.forif_backend.application.notification.dto.NotificationRecipientTarget;
import org.forif_backend.application.notification.dto.SendAlimTalkCommand;
import org.forif_backend.application.notification.dto.SendAlimTalkResult;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.user.UserService;
import org.forif_backend.common.dto.response.CursorPageResponse;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceRecipientTest {

    private static final Long SENDER_ID = 20260001L;
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

    @Test
    void normalizesReceiversBeforeLookingUpNamesAndSending() {
        User receiver = User.createUser(
                20260002L, "수신자", "receiver@hanyang.ac.kr", "01012345678", "컴퓨터학부");
        when(staffAccountRepository.findByUserId(SENDER_ID)).thenReturn(Optional.of(mock(StaffAccount.class)));
        when(userRepository.findByPhoneNum("01012345678")).thenReturn(Optional.of(receiver));
        when(notificationSendPort.sendAlimTalk(any(), any())).thenReturn(CompletableFuture.completedFuture(
                new SendAlimTalkResult("template", List.of())));

        notificationService.sendAlimTalk(
                new SendAlimTalkCommand(List.of("010-1234-5678"), "template", Map.of()),
                SENDER_ID
        ).join();

        verify(userRepository).findByPhoneNum("01012345678");
        verify(notificationSendPort).sendAlimTalk(
                eq(new SendAlimTalkCommand(List.of("01012345678"), "template", Map.of())),
                eq(Map.of("01012345678", "수신자"))
        );
    }
}
