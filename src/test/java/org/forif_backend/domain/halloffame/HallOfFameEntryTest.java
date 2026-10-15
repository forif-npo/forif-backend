package org.forif_backend.domain.halloffame;

import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 전시 항목의 주소 형식, 게시 상태 전이, 배지·성취 개수 제한.
 */
class HallOfFameEntryTest {

    private HallOfFameEntry entry() {
        return HallOfFameEntry.create(null, "홍길동", 2026, 2, "hong-gildong", "소개 한 줄", "본문", 1);
    }

    @Test
    void 주소는_영문_소문자와_숫자_하이픈만_허용한다() {
        assertThat(entry().getSlug()).isEqualTo("hong-gildong");

        List<String> invalid = List.of("Hong", "hong_gildong", "-hong", "hong-", "ab", "홍길동");
        invalid.forEach(slug ->
                assertThatThrownBy(() -> HallOfFameEntry.create(null, "홍길동", 2026, 2, slug, null, null, 0))
                        .isInstanceOf(ForifException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT));
    }

    @Test
    void 초안은_게시_전까지_공개되지_않는다() {
        HallOfFameEntry entry = entry();

        assertThat(entry.getStatus()).isEqualTo(HallOfFameStatus.DRAFT);
        assertThat(entry.isPublished()).isFalse();
        assertThat(entry.getPublishedAt()).isNull();
    }

    @Test
    void 게시하면_게시_시각이_기록된다() {
        HallOfFameEntry entry = entry();
        LocalDateTime publishedAt = LocalDateTime.of(2026, 10, 15, 10, 0);

        entry.publish(publishedAt);

        assertThat(entry.getStatus()).isEqualTo(HallOfFameStatus.PUBLISHED);
        assertThat(entry.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(entry.isPublished()).isTrue();
    }

    @Test
    void 이미_게시된_항목은_다시_게시할_수_없다() {
        HallOfFameEntry entry = entry();
        entry.publish(LocalDateTime.now());

        assertThatThrownBy(() -> entry.publish(LocalDateTime.now()))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HALL_OF_FAME_INVALID_STATUS);
    }

    @Test
    void 게시_중단_후_다시_게시하면_게시_시각이_갱신된다() {
        HallOfFameEntry entry = entry();
        LocalDateTime first = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime second = LocalDateTime.of(2026, 10, 15, 9, 0);

        entry.publish(first);
        entry.unpublish();
        assertThat(entry.getStatus()).isEqualTo(HallOfFameStatus.UNPUBLISHED);
        assertThat(entry.isPublished()).isFalse();

        entry.publish(second);
        assertThat(entry.getPublishedAt()).isEqualTo(second);
    }

    @Test
    void 게시되지_않은_항목은_게시_중단할_수_없다() {
        HallOfFameEntry entry = entry();

        assertThatThrownBy(entry::unpublish)
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HALL_OF_FAME_INVALID_STATUS);
    }

    @Test
    void 배지는_열_개까지_각_서른_자까지다() {
        HallOfFameEntry entry = entry();

        entry.replaceBadges(List.of("해커톤 대상", "멘토"));
        assertThat(entry.getBadges()).containsExactly("해커톤 대상", "멘토");

        List<String> tooMany = IntStream.rangeClosed(1, 11).mapToObj(i -> "배지" + i).toList();
        assertThatThrownBy(() -> entry.replaceBadges(tooMany))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

        List<String> tooLong = List.of("가".repeat(31));
        assertThatThrownBy(() -> entry.replaceBadges(tooLong))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void 성취는_스무_개까지이고_항목에_연결된다() {
        HallOfFameEntry entry = entry();

        entry.replaceAchievements(List.of(
                HallOfFameAchievement.create("해커톤 대상", "2026-1 해커톤", "https://forif.org", 1)));

        assertThat(entry.getAchievements()).hasSize(1);
        assertThat(entry.getAchievements().get(0).getEntry()).isSameAs(entry);

        List<HallOfFameAchievement> tooMany = IntStream.rangeClosed(1, 21)
                .mapToObj(i -> HallOfFameAchievement.create("성취" + i, null, null, i))
                .toList();
        assertThatThrownBy(() -> entry.replaceAchievements(tooMany))
                .isInstanceOf(ForifException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }
}
