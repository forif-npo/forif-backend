package org.forif_backend.application.staffrecruitment;

import org.forif_backend.domain.staffrecruitment.StaffRecruitment;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentRepository;
import org.forif_backend.domain.staffrecruitment.StaffRecruitmentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffRecruitmentAutoCloseSchedulerTest {

    @Mock
    private StaffRecruitmentRepository staffRecruitmentRepository;

    @InjectMocks
    private StaffRecruitmentAutoCloseScheduler scheduler;

    @Test
    void 종료된_게시_회차를_자연_마감한다() {
        LocalDateTime endsAt = LocalDateTime.of(2026, 11, 24, 0, 0);
        StaffRecruitment recruitment = StaffRecruitment.create(2027, 1, "운영진 모집", null, null,
                endsAt.minusDays(1), endsAt);
        recruitment.publish();
        when(staffRecruitmentRepository.findExpiredPublishedForUpdate(endsAt))
                .thenReturn(Optional.of(recruitment));

        scheduler.closeExpiredRecruitment(endsAt);

        assertThat(recruitment.getStatus()).isEqualTo(StaffRecruitmentStatus.CLOSED);
        assertThat(recruitment.getEndedAt()).isEqualTo(endsAt);
        assertThat(recruitment.getCloseReason()).isEqualTo("모집 기간 종료");
        assertThat(recruitment.getPublishedSlot()).isNull();
    }
}
