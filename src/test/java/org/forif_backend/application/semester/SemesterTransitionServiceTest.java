package org.forif_backend.application.semester;

import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.application.staff.StaffAccountService;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemesterTransitionServiceTest {

    private static final Long REQUESTER = 20260001L;
    private static final Long NEXT_PRESIDENT = 20260002L;
    private static final Long NEXT_VICE_PRESIDENT = 20260003L;

    @Mock private SemesterService semesterService;
    @Mock private StaffAccountService staffAccountService;
    @InjectMocks private SemesterTransitionService service;

    @Test
    void transfersDesignatedTeamBeforeChangingActiveSemester() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(staffAccountService.isAdminAccount(NEXT_PRESIDENT)).thenReturn(true);
        when(staffAccountService.getDesignatedNextPresidentUserId(2027, 1)).thenReturn(NEXT_PRESIDENT);
        when(staffAccountService.getDesignatedNextVicePresidentUserId(2027, 1)).thenReturn(NEXT_VICE_PRESIDENT);
        when(semesterService.changeActive(2027, 1, REQUESTER)).thenReturn(SemesterInfo.of(2027, 1));

        assertEquals(SemesterInfo.of(2027, 1), service.transition(2027, 1, NEXT_PRESIDENT, REQUESTER));

        var order = inOrder(staffAccountService, semesterService);
        order.verify(staffAccountService).handOverPresidency(REQUESTER, NEXT_PRESIDENT);
        order.verify(staffAccountService).handOverVicePresidency(NEXT_VICE_PRESIDENT);
        order.verify(semesterService).changeActive(2027, 1, REQUESTER);
    }

    @Test
    void rejectsPresidentThatDiffersFromNextSemesterAppointment() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(staffAccountService.isAdminAccount(NEXT_PRESIDENT)).thenReturn(true);
        when(staffAccountService.getDesignatedNextPresidentUserId(2027, 1)).thenReturn(20260004L);

        ForifException denied = assertThrows(ForifException.class,
                () -> service.transition(2027, 1, NEXT_PRESIDENT, REQUESTER));

        assertEquals(ErrorCode.SEMESTER_NEXT_PRESIDENT_MISMATCH, denied.getErrorCode());
        verify(semesterService, never()).changeActive(2027, 1, REQUESTER);
    }

    @Test
    void rejectsVicePresidentAsPresidentHandoverTarget() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(staffAccountService.isAdminAccount(NEXT_PRESIDENT)).thenReturn(true);
        when(staffAccountService.getDesignatedNextPresidentUserId(2027, 1)).thenReturn(null);
        when(staffAccountService.getDesignatedNextVicePresidentUserId(2027, 1)).thenReturn(NEXT_PRESIDENT);

        ForifException denied = assertThrows(ForifException.class,
                () -> service.transition(2027, 1, NEXT_PRESIDENT, REQUESTER));

        assertEquals(ErrorCode.SEMESTER_NEXT_TEAM_CONFLICT, denied.getErrorCode());
        verify(semesterService, never()).changeActive(2027, 1, REQUESTER);
    }

    @Test
    void upcomingPresidentCannotTransitionEvenToTheirDesignatedSemester() {
        doThrow(new ForifException(ErrorCode.INSUFFICIENT_PERMISSION))
                .when(staffAccountService).requireIncumbentPresident(REQUESTER);

        ForifException denied = assertThrows(ForifException.class,
                () -> service.transition(2027, 1, REQUESTER, REQUESTER));

        assertEquals(ErrorCode.INSUFFICIENT_PERMISSION, denied.getErrorCode());
        verify(staffAccountService, never()).handOverPresidency(anyLong(), anyLong());
        verify(semesterService, never()).changeActive(anyInt(), anyInt(), anyLong());
    }

    @Test
    void sameSemesterRequestDoesNotHandOverLeadership() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));

        ForifException denied = assertThrows(ForifException.class,
                () -> service.transition(2026, 2, REQUESTER, REQUESTER));

        assertEquals(ErrorCode.SEMESTER_INVALID, denied.getErrorCode());
        verify(staffAccountService, never()).handOverPresidency(anyLong(), anyLong());
        verify(staffAccountService, never()).handOverVicePresidency(nullable(Long.class));
        verify(semesterService, never()).changeActive(anyInt(), anyInt(), anyLong());
    }

    @Test
    void nextSemesterWithoutVicePresidentStillEndsIncumbentVicePresidency() {
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));
        when(staffAccountService.isAdminAccount(NEXT_PRESIDENT)).thenReturn(true);
        when(staffAccountService.getDesignatedNextPresidentUserId(2027, 1)).thenReturn(NEXT_PRESIDENT);
        when(staffAccountService.getDesignatedNextVicePresidentUserId(2027, 1)).thenReturn(null);
        when(semesterService.changeActive(2027, 1, REQUESTER)).thenReturn(SemesterInfo.of(2027, 1));

        service.transition(2027, 1, NEXT_PRESIDENT, REQUESTER);

        verify(staffAccountService).handOverVicePresidency(null);
        verify(semesterService).changeActive(2027, 1, REQUESTER);
    }
}
