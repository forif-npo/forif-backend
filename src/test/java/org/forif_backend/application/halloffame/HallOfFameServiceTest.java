package org.forif_backend.application.halloffame;

import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.halloffame.dto.HallOfFameCommands.AchievementCommand;
import org.forif_backend.application.halloffame.dto.HallOfFameCommands.CreateCommand;
import org.forif_backend.application.halloffame.dto.HallOfFameCommands.UpdateCommand;
import org.forif_backend.application.halloffame.dto.HallOfFameEntryDetail;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.halloffame.HallOfFameEntry;
import org.forif_backend.domain.halloffame.HallOfFameRepository;
import org.forif_backend.domain.halloffame.HallOfFameStatus;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HallOfFameServiceTest {

    private static final Integer ENTRY_ID = 7;
    private static final Long MEMBER_ID = 2020123456L;

    @Mock
    private HallOfFameRepository hallOfFameRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilePort filePort;

    @InjectMocks
    private HallOfFameService hallOfFameService;

    private final User member =
            User.createUser(MEMBER_ID, "홍길동", "hong@hanyang.ac.kr", "01000000000", "컴퓨터소프트웨어학부");

    private HallOfFameEntry entry(boolean published) {
        HallOfFameEntry entry = HallOfFameEntry.create(member, "홍길동", 2026, 2, "hong-gildong", "소개", "본문", 1);
        if (published) {
            entry.publish(LocalDateTime.now().minusDays(1));
        }
        ReflectionTestUtils.setField(entry, "id", ENTRY_ID);
        ReflectionTestUtils.setField(entry, "version", 3L);
        return entry;
    }

    private CreateCommand createCommand(List<AchievementCommand> achievements) {
        return new CreateCommand(MEMBER_ID, null, 2026, 2, "hong-gildong", "소개", "본문", 1,
                List.of("해커톤 대상"), achievements);
    }

    @Test
    void 표시_이름을_비우면_연결된_회원_이름을_복사한다() {
        when(hallOfFameRepository.existsBySlug("hong-gildong")).thenReturn(false);
        when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(hallOfFameRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        HallOfFameEntryDetail detail = hallOfFameService.create(createCommand(List.of(
                new AchievementCommand("해커톤 대상", "2026-1", "https://forif.org", 1))));

        assertThat(detail.memberName()).isEqualTo("홍길동");
        assertThat(detail.memberUserId()).isEqualTo(MEMBER_ID);
        assertThat(detail.status()).isEqualTo(HallOfFameStatus.DRAFT);
        assertThat(detail.semesterLabel()).isEqualTo("26-2");
        assertThat(detail.badges()).containsExactly("해커톤 대상");
        assertThat(detail.achievements()).hasSize(1);
    }

    @Test
    void 이미_쓰는_주소로는_만들_수_없다() {
        when(hallOfFameRepository.existsBySlug("hong-gildong")).thenReturn(true);

        assertThatThrownBy(() -> hallOfFameService.create(createCommand(List.of())))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HALL_OF_FAME_SLUG_ALREADY_EXISTS);
    }

    @Test
    void 성취_링크는_https만_허용한다() {
        when(hallOfFameRepository.existsBySlug("hong-gildong")).thenReturn(false);
        when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> hallOfFameService.create(createCommand(List.of(
                new AchievementCommand("해커톤 대상", null, "http://forif.org", 1)))))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void 화면이_가진_version이_낡았으면_수정을_거절한다() {
        when(hallOfFameRepository.findDetailById(ENTRY_ID)).thenReturn(Optional.of(entry(false)));

        assertThatThrownBy(() -> hallOfFameService.update(ENTRY_ID, new UpdateCommand(
                1L, MEMBER_ID, "홍길동", 2026, 2, "hong-gildong", "바뀐 소개", "본문", 1, List.of(), List.of())))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_INTEGRITY_VIOLATION);
    }

    @Test
    void 이미_게시된_항목은_다시_게시할_수_없다() {
        when(hallOfFameRepository.findDetailById(ENTRY_ID)).thenReturn(Optional.of(entry(true)));

        assertThatThrownBy(() -> hallOfFameService.publish(ENTRY_ID, 3L))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HALL_OF_FAME_INVALID_STATUS);
    }

    @Test
    void 게시_중단하면_공개_목록에서_빠진다() {
        when(hallOfFameRepository.findDetailById(ENTRY_ID)).thenReturn(Optional.of(entry(true)));

        HallOfFameEntryDetail detail = hallOfFameService.unpublish(ENTRY_ID, 3L);

        assertThat(detail.status()).isEqualTo(HallOfFameStatus.UNPUBLISHED);
    }

    @Test
    void 게시되지_않은_주소는_공개_조회에서_404다() {
        when(hallOfFameRepository.findPublishedBySlug("draft-entry")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hallOfFameService.getPublishedBySlug("draft-entry"))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HALL_OF_FAME_ENTRY_NOT_FOUND);
    }

    @Test
    void 없는_항목은_관리_조회에서도_404다() {
        when(hallOfFameRepository.findDetailById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hallOfFameService.getForAdmin(999))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HALL_OF_FAME_ENTRY_NOT_FOUND);
    }
}
