package org.forif_backend.domain.halloffame;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.user.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 명예의 전당 전시 항목.
 *
 * 관리자가 직접 등록하고 게시한다. 회원·학기는 기존 데이터를 참조하되, 게시된 소개가 유지되도록
 * 이름은 값으로 복사해 둔다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_hall_of_fame_entry")
public class HallOfFameEntry extends BaseTimeEntity {

    public static final int MAX_BADGES = 10;
    public static final int MAX_BADGE_LENGTH = 30;
    public static final int MAX_ACHIEVEMENTS = 20;

    /** 영문 소문자·숫자·하이픈 3~80자, 앞뒤 하이픈 금지 */
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    private static final int SLUG_MIN_LENGTH = 3;
    private static final int SLUG_MAX_LENGTH = 80;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entry_id")
    private Integer id;

    /** 탈퇴하면 null이 된다. 표시용 이름은 snapshot으로 남는다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User member;

    @Column(name = "member_name", nullable = false, length = 50)
    private String memberName;

    @Column(name = "act_year", nullable = false)
    private int actYear;

    @Column(name = "act_semester", nullable = false)
    private int actSemester;

    @Column(nullable = false, length = 80)
    private String slug;

    @Column(length = 200)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "image_object_key", length = 300)
    private String imageObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HallOfFameStatus status;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @ElementCollection
    @CollectionTable(name = "tb_hall_of_fame_badge", joinColumns = @JoinColumn(name = "entry_id"))
    @OrderColumn(name = "display_order")
    @Column(name = "badge", nullable = false, length = MAX_BADGE_LENGTH)
    private List<String> badges = new ArrayList<>();

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<HallOfFameAchievement> achievements = new ArrayList<>();

    private HallOfFameEntry(User member, String memberName, int actYear, int actSemester, String slug,
                            String headline, String introduction, int displayOrder) {
        this.member = member;
        this.memberName = memberName;
        this.actYear = actYear;
        this.actSemester = actSemester;
        this.slug = slug;
        this.headline = headline;
        this.introduction = introduction;
        this.displayOrder = displayOrder;
        this.status = HallOfFameStatus.DRAFT;
    }

    public static HallOfFameEntry create(User member, String memberName, int actYear, int actSemester, String slug,
                                         String headline, String introduction, int displayOrder) {
        validateSlug(slug);
        return new HallOfFameEntry(member, memberName, actYear, actSemester, slug,
                headline, introduction, displayOrder);
    }

    public void updateContent(String memberName, int actYear, int actSemester, String slug,
                              String headline, String introduction, int displayOrder) {
        validateSlug(slug);
        this.memberName = memberName;
        this.actYear = actYear;
        this.actSemester = actSemester;
        this.slug = slug;
        this.headline = headline;
        this.introduction = introduction;
        this.displayOrder = displayOrder;
    }

    public void replaceBadges(List<String> next) {
        if (next.size() > MAX_BADGES) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
        next.forEach(badge -> {
            if (badge == null || badge.isBlank() || badge.length() > MAX_BADGE_LENGTH) {
                throw new ForifException(ErrorCode.INVALID_INPUT);
            }
        });
        this.badges.clear();
        this.badges.addAll(next);
    }

    public void replaceAchievements(List<HallOfFameAchievement> next) {
        if (next.size() > MAX_ACHIEVEMENTS) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
        this.achievements.clear();
        next.forEach(achievement -> {
            achievement.assignTo(this);
            this.achievements.add(achievement);
        });
    }

    public void updateImage(String imageObjectKey) {
        this.imageObjectKey = imageObjectKey;
    }

    /** 최초 게시와 재게시 모두 게시 시각을 갱신한다. */
    public void publish(LocalDateTime now) {
        if (this.status == HallOfFameStatus.PUBLISHED) {
            throw new ForifException(ErrorCode.HALL_OF_FAME_INVALID_STATUS);
        }
        this.status = HallOfFameStatus.PUBLISHED;
        this.publishedAt = now;
    }

    public void unpublish() {
        if (this.status != HallOfFameStatus.PUBLISHED) {
            throw new ForifException(ErrorCode.HALL_OF_FAME_INVALID_STATUS);
        }
        this.status = HallOfFameStatus.UNPUBLISHED;
    }

    public boolean isPublished() {
        return this.status.isPublished();
    }

    private static void validateSlug(String slug) {
        if (slug == null
                || slug.length() < SLUG_MIN_LENGTH
                || slug.length() > SLUG_MAX_LENGTH
                || !SLUG_PATTERN.matcher(slug).matches()) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
    }
}
