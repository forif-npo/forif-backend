package org.forif_backend.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    /** 유니크 제약 위반을 서비스 계층에서 즉시 처리할 수 있도록 변경을 DB에 반영한다. */
    void flush();

    Optional<Product> findById(Integer productId);

    /** 수정·삭제·승인처럼 상태를 바꾸는 작업에서 행을 잠근 채 조회한다. */
    Optional<Product> findByIdForUpdate(Integer productId);

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /** 신청자의 특정 상태 서비스 수 (검토 대기 제한 검사용) */
    long countByApplicantIdAndStatus(Long userId, ProductStatus status);

    /** 게시된(승인 이후) 서비스 목록 — 최신 연도순 */
    List<Product> findAllPublished();

    /** 신청자 기준 전체 신청/게시 목록 — 최신 신청순 */
    List<Product> findAllByApplicantId(Long userId);

    /** 어드민용 전체 목록 (상태 무관) — 최신 신청순 */
    List<Product> findAll();

    void delete(Product product);
}
