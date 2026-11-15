package org.forif_backend.domain.halloffame;

/**
 * 전시 항목의 게시 상태.
 * 초안과 게시 중단은 공개 API에 노출하지 않는다.
 */
public enum HallOfFameStatus {
    DRAFT,
    PUBLISHED,
    UNPUBLISHED;

    public boolean isPublished() {
        return this == PUBLISHED;
    }
}
