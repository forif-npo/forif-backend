package org.forif_backend.application.semester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.semester.SemesterPhase;
import org.forif_backend.domain.semester.SemesterSchedule;
import org.forif_backend.domain.semester.SemesterScheduleRepository;
import org.forif_backend.domain.user.UserApplyRepository;
import org.junit.jupiter.api.Test;

class SemesterScheduleServiceTest {

    private final SemesterScheduleRepository semesterScheduleRepository = mock(SemesterScheduleRepository.class);
    private final SemesterService semesterService = mock(SemesterService.class);
    private final UserApplyRepository userApplyRepository = mock(UserApplyRepository.class);
    private final SemesterScheduleService service = new SemesterScheduleService(
            semesterScheduleRepository, semesterService, userApplyRepository);

    @Test
    void rejectsScheduleTimesMorePreciseThanMinutes() {
        List<SemesterScheduleService.PhaseWindow> windows = List.of(
                new SemesterScheduleService.PhaseWindow(
                        SemesterPhase.MENTOR_RECRUIT,
                        LocalDateTime.of(2026, 3, 2, 9, 30, 1),
                        LocalDateTime.of(2026, 3, 2, 10, 0)
                ));

        assertThatThrownBy(() -> service.replaceSchedules(2026, 1, windows, 1L))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.SEMESTER_SCHEDULE_INVALID_RANGE));
    }

    @Test
    void deletingEndedCurrentMenteeReviewRejectsPendingApplications() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        SemesterSchedule menteeReview = mock(SemesterSchedule.class);
        when(semesterScheduleRepository.findByYearAndSemesterForUpdate(2026, 2))
                .thenReturn(List.of(menteeReview));
        when(menteeReview.getPhase()).thenReturn(SemesterPhase.MENTEE_REVIEW);
        when(menteeReview.getEndsAt()).thenReturn(now.minusMinutes(1));
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));

        service.replaceSchedules(2026, 2, List.of(), 1L);

        verify(semesterScheduleRepository).delete(menteeReview);
        verify(userApplyRepository).rejectPendingApplicationsByYearSemester(2026, 2);
    }

    @Test
    void deletingCurrentMenteeReviewBeforeItEndsDoesNotRejectPendingApplications() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        SemesterSchedule menteeReview = mock(SemesterSchedule.class);
        when(semesterScheduleRepository.findByYearAndSemesterForUpdate(2026, 2))
                .thenReturn(List.of(menteeReview));
        when(menteeReview.getPhase()).thenReturn(SemesterPhase.MENTEE_REVIEW);
        when(menteeReview.getEndsAt()).thenReturn(now.plusMinutes(1));
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));

        service.replaceSchedules(2026, 2, List.of(), 1L);

        verify(semesterScheduleRepository).delete(menteeReview);
        verify(userApplyRepository, never()).rejectPendingApplicationsByYearSemester(2026, 2);
    }

    @Test
    void deletingPastOrFutureMenteeReviewDoesNotChangeApplications() {
        SemesterSchedule menteeReview = mock(SemesterSchedule.class);
        when(semesterScheduleRepository.findByYearAndSemesterForUpdate(2026, 1))
                .thenReturn(List.of(menteeReview));
        when(menteeReview.getPhase()).thenReturn(SemesterPhase.MENTEE_REVIEW);
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));

        service.replaceSchedules(2026, 1, List.of(), 1L);

        verify(semesterScheduleRepository).delete(menteeReview);
        verify(userApplyRepository, never()).rejectPendingApplicationsByYearSemester(2026, 1);
    }

    @Test
    void doesNotAllowStartedCurrentMenteeReviewToBeExtended() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul")).withSecond(0).withNano(0);
        SemesterSchedule menteeReview = mock(SemesterSchedule.class);
        when(semesterScheduleRepository.findByYearAndSemesterForUpdate(2026, 2))
                .thenReturn(List.of(menteeReview));
        when(menteeReview.getPhase()).thenReturn(SemesterPhase.MENTEE_REVIEW);
        when(menteeReview.getStartsAt()).thenReturn(now.minusDays(1));
        when(menteeReview.getEndsAt()).thenReturn(now.plusMinutes(1));
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));

        List<SemesterScheduleService.PhaseWindow> windows = List.of(
                new SemesterScheduleService.PhaseWindow(
                        SemesterPhase.MENTEE_REVIEW,
                        now.minusDays(1),
                        now.plusDays(1)
                ));

        assertThatThrownBy(() -> service.replaceSchedules(2026, 2, windows, 1L))
                .isInstanceOf(ForifException.class)
                .satisfies(exception -> assertThat(((ForifException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.SEMESTER_PHASE_CLOSED));

        verify(semesterScheduleRepository, never()).save(menteeReview);
        verify(semesterScheduleRepository, never()).delete(menteeReview);
        verify(userApplyRepository, never()).rejectPendingApplicationsByYearSemester(2026, 2);
    }

    @Test
    void allowsExtendingMenteeReviewBeforeItStarts() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul")).withSecond(0).withNano(0);
        SemesterSchedule menteeReview = SemesterSchedule.create(
                2026, 2, SemesterPhase.MENTEE_REVIEW, now.plusDays(1), now.plusDays(2), 1L);
        when(semesterScheduleRepository.findByYearAndSemesterForUpdate(2026, 2))
                .thenReturn(List.of(menteeReview));
        when(semesterScheduleRepository.save(menteeReview)).thenReturn(menteeReview);
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));

        List<SemesterScheduleService.PhaseWindow> windows = List.of(
                new SemesterScheduleService.PhaseWindow(
                        SemesterPhase.MENTEE_REVIEW,
                        now.plusDays(1),
                        now.plusDays(3)
                ));

        service.replaceSchedules(2026, 2, windows, 1L);

        assertThat(menteeReview.getEndsAt()).isEqualTo(now.plusDays(3));
        verify(userApplyRepository, never()).rejectPendingApplicationsByYearSemester(2026, 2);
    }

    @Test
    void allowsExtendingMenteeReviewForFutureSemester() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul")).withSecond(0).withNano(0);
        SemesterSchedule menteeReview = SemesterSchedule.create(
                2027, 1, SemesterPhase.MENTEE_REVIEW, now.minusDays(1), now.plusDays(1), 1L);
        when(semesterScheduleRepository.findByYearAndSemesterForUpdate(2027, 1))
                .thenReturn(List.of(menteeReview));
        when(semesterScheduleRepository.save(menteeReview)).thenReturn(menteeReview);
        when(semesterService.getActive()).thenReturn(SemesterInfo.of(2026, 2));

        List<SemesterScheduleService.PhaseWindow> windows = List.of(
                new SemesterScheduleService.PhaseWindow(
                        SemesterPhase.MENTEE_REVIEW,
                        now.minusDays(1),
                        now.plusDays(2)
                ));

        service.replaceSchedules(2027, 1, windows, 1L);

        assertThat(menteeReview.getEndsAt()).isEqualTo(now.plusDays(2));
    }
}
