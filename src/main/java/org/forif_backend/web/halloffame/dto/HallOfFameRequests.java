package org.forif_backend.web.halloffame.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.forif_backend.application.halloffame.dto.HallOfFameCommands.AchievementCommand;
import org.forif_backend.application.halloffame.dto.HallOfFameCommands.CreateCommand;
import org.forif_backend.application.halloffame.dto.HallOfFameCommands.UpdateCommand;

import java.util.List;

/**
 * 전시 항목 관리 요청.
 * 배지와 성취는 전체 목록을 보내 교체한다.
 */
public final class HallOfFameRequests {

    /** 영문 소문자·숫자·하이픈. 앞뒤 하이픈 금지 */
    private static final String SLUG_REGEX = "^[a-z0-9]+(?:-[a-z0-9]+)*$";

    private HallOfFameRequests() {
    }

    public record AchievementRequest(
            @NotBlank @Size(max = 100) String title,
            @Size(max = 500) String description,
            @Size(max = 300) String linkUrl,
            @Min(0) int displayOrder
    ) {
        AchievementCommand toCommand() {
            return new AchievementCommand(title, description, linkUrl, displayOrder);
        }
    }

    public record CreateRequest(
            Long memberUserId,
            @Size(max = 50) String memberName,
            @Min(2000) int actYear,
            @Min(1) @Max(2) int actSemester,
            @NotBlank @Size(min = 3, max = 80) @Pattern(regexp = SLUG_REGEX) String slug,
            @Size(max = 200) String headline,
            String introduction,
            @Min(0) int displayOrder,
            @Size(max = 10) List<@Size(max = 30) String> badges,
            @Size(max = 20) @Valid List<AchievementRequest> achievements
    ) {
        public CreateCommand toCommand() {
            return new CreateCommand(memberUserId, memberName, actYear, actSemester, slug, headline, introduction,
                    displayOrder,
                    badges == null ? List.of() : badges,
                    achievements == null ? List.of()
                            : achievements.stream().map(AchievementRequest::toCommand).toList());
        }
    }

    public record UpdateRequest(
            @NotNull Long version,
            Long memberUserId,
            @Size(max = 50) String memberName,
            @Min(2000) int actYear,
            @Min(1) @Max(2) int actSemester,
            @NotBlank @Size(min = 3, max = 80) @Pattern(regexp = SLUG_REGEX) String slug,
            @Size(max = 200) String headline,
            String introduction,
            @Min(0) int displayOrder,
            @Size(max = 10) List<@Size(max = 30) String> badges,
            @Size(max = 20) @Valid List<AchievementRequest> achievements
    ) {
        public UpdateCommand toCommand() {
            return new UpdateCommand(version, memberUserId, memberName, actYear, actSemester, slug, headline,
                    introduction, displayOrder,
                    badges == null ? List.of() : badges,
                    achievements == null ? List.of()
                            : achievements.stream().map(AchievementRequest::toCommand).toList());
        }
    }

    /** 게시·게시 중단 공통 본문 */
    public record ActionRequest(
            @NotNull Long version
    ) {
    }
}
