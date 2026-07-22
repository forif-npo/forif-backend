package org.forif_backend.domain.product;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.forif_backend.common.BaseTimeEntity;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.user.User;

import java.util.ArrayList;
import java.util.List;

/**
 * 부원이 만든 프로덕트(서비스).
 * 등록 신청(PENDING/REJECTED)과 승인 후 게시(LIVE/DEV/PAUSED/RETIRED)를 하나의 엔티티로 관리한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tb_product")
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Integer id;

    /** 서브도메인으로 쓰이는 식별자 ({slug}.forif.org) */
    @Column(nullable = false, unique = true, length = 30)
    private String slug;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "one_liner", nullable = false, length = 200)
    private String oneLiner;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private ProductSourceType sourceType;

    /** 출처 표기 라벨 (예: "2026-1 해커톤 대상") */
    @Column(name = "source_label", length = 100)
    private String sourceLabel;

    /** 쉼표로 구분된 태그 목록 */
    @Column(length = 200)
    private String tags;

    /** 쉼표로 구분된 기술 스택 목록 */
    @Column(name = "tech_stack", length = 300)
    private String techStack;

    @Column(name = "service_url", length = 300)
    private String serviceUrl;

    @Column(name = "github_url", length = 300)
    private String githubUrl;

    @Column(name = "thumbnail_object_key", length = 300)
    private String thumbnailObjectKey;

    @Column(name = "act_year", nullable = false)
    private int actYear;

    /** 등록을 신청한 부원 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_user_id", nullable = false)
    private User applicant;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductMember> members = new ArrayList<>();

    public static Product createPending(String slug, String name, String oneLiner, String description,
                                        ProductSourceType sourceType, String tags, String techStack,
                                        String serviceUrl, String githubUrl, int actYear, User applicant) {
        Product product = new Product();
        product.slug = slug;
        product.name = name;
        product.oneLiner = oneLiner;
        product.description = description;
        product.status = ProductStatus.PENDING;
        product.sourceType = sourceType;
        product.tags = tags;
        product.techStack = techStack;
        product.serviceUrl = serviceUrl;
        product.githubUrl = githubUrl;
        product.actYear = actYear;
        product.applicant = applicant;
        return product;
    }

    public void addMember(ProductMember member) {
        members.add(member);
    }

    public void approve() {
        if (status != ProductStatus.PENDING) {
            throw new ForifException(ErrorCode.PRODUCT_NOT_PENDING);
        }
        this.status = ProductStatus.LIVE;
        this.rejectReason = null;
    }

    public void reject(String reason) {
        if (status != ProductStatus.PENDING) {
            throw new ForifException(ErrorCode.PRODUCT_NOT_PENDING);
        }
        this.status = ProductStatus.REJECTED;
        this.rejectReason = reason;
    }

    public void changeStatus(ProductStatus newStatus) {
        if (!this.status.isPublished() || !newStatus.isPublished()) {
            throw new ForifException(ErrorCode.PRODUCT_STATUS_NOT_CHANGEABLE);
        }
        this.status = newStatus;
    }

    public void updateSourceLabel(String sourceLabel) {
        this.sourceLabel = sourceLabel;
    }
}
