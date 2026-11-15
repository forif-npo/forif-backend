package org.forif_backend.domain.halloffame;

import java.util.List;
import java.util.Optional;

public interface HallOfFameRepository {

    HallOfFameEntry save(HallOfFameEntry entry);

    /** slug 유니크 위반을 서비스 계층에서 바로 처리할 수 있도록 변경을 DB에 반영한다. */
    void flush();

    Optional<HallOfFameEntry> findById(Integer entryId);

    /** 성취까지 함께 읽는 상세 조회 */
    Optional<HallOfFameEntry> findDetailById(Integer entryId);

    /** 게시된 항목만 slug로 조회한다. 초안·게시 중단은 비어 있다. */
    Optional<HallOfFameEntry> findPublishedBySlug(String slug);

    boolean existsBySlug(String slug);

    /** 관리 목록 — 상태 필터는 null이면 전체, 정렬은 display_order ASC, entry_id DESC */
    List<HallOfFameEntry> findAllForAdmin(HallOfFameStatus status, int offset, int limit);

    long countForAdmin(HallOfFameStatus status);

    /** 공개 목록 — 게시된 항목만, 시즌 필터는 null이면 전체 */
    List<HallOfFameEntry> findPublished(Integer actYear, Integer actSemester, int offset, int limit);

    long countPublished(Integer actYear, Integer actSemester);
}
