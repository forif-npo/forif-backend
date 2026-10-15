package org.forif_backend.application.halloffame.dto;

import org.forif_backend.domain.halloffame.HallOfFameEntry;
import org.forif_backend.domain.halloffame.HallOfFameStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 전시 항목 조회 모델. 공개 응답과 관리 응답이 같은 모델을 쓰고 노출 범위는 web 계층에서 가린다.
 */
public record HallOfFameEntryDetail(
        Integer entryId,
        Long memberUserId,
        String memberName,
        int actYear,
        int actSemester,
        String semesterLabel,
        String slug,
        String headline,
        String introduction,
        String imageUrl,
        HallOfFameStatus status,
        int displayOrder,
        LocalDateTime publishedAt,
        Long version,
        List<String> badges,
        List<AchievementInfo> achievements,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record AchievementInfo(
            Integer achievementId,
            String title,
            String description,
            String linkUrl,
            int displayOrder
    ) {
    }

    public static HallOfFameEntryDetail from(HallOfFameEntry entry, String imageUrl) {
        return new HallOfFameEntryDetail(
                entry.getId(),
                entry.getMember() == null ? null : entry.getMember().getId(),
                entry.getMemberName(),
                entry.getActYear(),
                entry.getActSemester(),
                "%02d-%d".formatted(entry.getActYear() % 100, entry.getActSemester()),
                entry.getSlug(),
                entry.getHeadline(),
                entry.getIntroduction(),
                imageUrl,
                entry.getStatus(),
                entry.getDisplayOrder(),
                entry.getPublishedAt(),
                entry.getVersion(),
                List.copyOf(entry.getBadges()),
                entry.getAchievements().stream()
                        .map(achievement -> new AchievementInfo(
                                achievement.getId(),
                                achievement.getTitle(),
                                achievement.getDescription(),
                                achievement.getLinkUrl(),
                                achievement.getDisplayOrder()))
                        .toList(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
