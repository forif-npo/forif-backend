package org.forif_backend.web.halloffame.dto;

import org.forif_backend.application.halloffame.dto.HallOfFameEntryDetail;
import org.forif_backend.domain.halloffame.HallOfFameStatus;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 전시 항목 응답.
 * 공개 응답에는 관리 전용 값(상태·노출 순서·version)과 회원 식별자를 넣지 않는다.
 */
public final class HallOfFameResponse {

    private static final ZoneId KOREA_STANDARD_TIME = ZoneId.of("Asia/Seoul");

    private HallOfFameResponse() {
    }

    public record AchievementResponse(
            Integer achievementId,
            String title,
            String description,
            String linkUrl,
            int displayOrder
    ) {
        static AchievementResponse from(HallOfFameEntryDetail.AchievementInfo info) {
            return new AchievementResponse(info.achievementId(), info.title(), info.description(),
                    info.linkUrl(), info.displayOrder());
        }
    }

    /** 공개 목록·상세 응답 */
    public record PublicResponse(
            Integer entryId,
            String slug,
            String memberName,
            int actYear,
            int actSemester,
            String semesterLabel,
            String headline,
            String introduction,
            String imageUrl,
            List<String> badges,
            List<AchievementResponse> achievements,
            OffsetDateTime publishedAt
    ) {
        public static PublicResponse from(HallOfFameEntryDetail detail) {
            return new PublicResponse(
                    detail.entryId(),
                    detail.slug(),
                    detail.memberName(),
                    detail.actYear(),
                    detail.actSemester(),
                    detail.semesterLabel(),
                    detail.headline(),
                    detail.introduction(),
                    detail.imageUrl(),
                    detail.badges(),
                    detail.achievements().stream().map(AchievementResponse::from).toList(),
                    toOffset(detail.publishedAt()));
        }
    }

    /** 관리 목록·상세·미리보기 응답 */
    public record AdminResponse(
            Integer entryId,
            String slug,
            Long memberUserId,
            String memberName,
            int actYear,
            int actSemester,
            String semesterLabel,
            String headline,
            String introduction,
            String imageUrl,
            HallOfFameStatus status,
            int displayOrder,
            Long version,
            List<String> badges,
            List<AchievementResponse> achievements,
            OffsetDateTime publishedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        public static AdminResponse from(HallOfFameEntryDetail detail) {
            return new AdminResponse(
                    detail.entryId(),
                    detail.slug(),
                    detail.memberUserId(),
                    detail.memberName(),
                    detail.actYear(),
                    detail.actSemester(),
                    detail.semesterLabel(),
                    detail.headline(),
                    detail.introduction(),
                    detail.imageUrl(),
                    detail.status(),
                    detail.displayOrder(),
                    detail.version(),
                    detail.badges(),
                    detail.achievements().stream().map(AchievementResponse::from).toList(),
                    toOffset(detail.publishedAt()),
                    toOffset(detail.createdAt()),
                    toOffset(detail.updatedAt()));
        }
    }

    /** 이미지 업로드 응답 */
    public record ImageResponse(String imageUrl) {
    }

    private static OffsetDateTime toOffset(LocalDateTime value) {
        return value == null ? null : value.atZone(KOREA_STANDARD_TIME).toOffsetDateTime();
    }
}
