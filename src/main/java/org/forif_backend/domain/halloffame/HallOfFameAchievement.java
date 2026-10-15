package org.forif_backend.domain.halloffame;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전시 항목의 성취 한 줄. 외부 링크는 https만 허용한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_hall_of_fame_achievement")
public class HallOfFameAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "achievement_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    private HallOfFameEntry entry;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "link_url", length = 300)
    private String linkUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    private HallOfFameAchievement(String title, String description, String linkUrl, int displayOrder) {
        this.title = title;
        this.description = description;
        this.linkUrl = linkUrl;
        this.displayOrder = displayOrder;
    }

    public static HallOfFameAchievement create(String title, String description, String linkUrl, int displayOrder) {
        return new HallOfFameAchievement(title, description, linkUrl, displayOrder);
    }

    void assignTo(HallOfFameEntry entry) {
        this.entry = entry;
    }
}
