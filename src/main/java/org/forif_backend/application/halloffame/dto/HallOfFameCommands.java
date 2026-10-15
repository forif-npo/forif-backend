package org.forif_backend.application.halloffame.dto;

import java.util.List;

/**
 * 명예의 전당 전시 항목 입력.
 * 배지와 성취는 전체 목록을 받아 교체한다.
 */
public final class HallOfFameCommands {

    private HallOfFameCommands() {
    }

    public record AchievementCommand(
            String title,
            String description,
            String linkUrl,
            int displayOrder
    ) {
    }

    public record CreateCommand(
            Long memberUserId,
            String memberName,
            int actYear,
            int actSemester,
            String slug,
            String headline,
            String introduction,
            int displayOrder,
            List<String> badges,
            List<AchievementCommand> achievements
    ) {
    }

    public record UpdateCommand(
            Long version,
            Long memberUserId,
            String memberName,
            int actYear,
            int actSemester,
            String slug,
            String headline,
            String introduction,
            int displayOrder,
            List<String> badges,
            List<AchievementCommand> achievements
    ) {
    }
}
