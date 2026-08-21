package org.forif_backend.infrastructure.persistence.product;

import jakarta.persistence.LockModeType;
import org.forif_backend.domain.product.Product;
import org.forif_backend.domain.product.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<Product, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Integer id);

    @Query("""
            SELECT DISTINCT p FROM Product p
            JOIN FETCH p.applicant
            LEFT JOIN FETCH p.members
            WHERE p.slug = :slug
            """)
    Optional<Product> findBySlug(@Param("slug") String slug);

    boolean existsBySlug(String slug);

    long countByApplicantIdAndStatus(Long applicantId, ProductStatus status);

    // 응답 변환(ProductInfo)에서 신청자와 팀원을 모두 사용하므로 함께 조회한다 (N+1 방지)
    @Query("""
            SELECT DISTINCT p FROM Product p
            JOIN FETCH p.applicant
            LEFT JOIN FETCH p.members
            WHERE p.status IN (org.forif_backend.domain.product.ProductStatus.LIVE,
                               org.forif_backend.domain.product.ProductStatus.DEV,
                               org.forif_backend.domain.product.ProductStatus.PAUSED,
                               org.forif_backend.domain.product.ProductStatus.RETIRED)
            ORDER BY p.actYear DESC, p.id DESC
            """)
    List<Product> findAllPublished();

    @Query("""
            SELECT DISTINCT p FROM Product p
            JOIN FETCH p.applicant
            LEFT JOIN FETCH p.members
            WHERE p.applicant.id = :userId
            ORDER BY p.id DESC
            """)
    List<Product> findAllByApplicantId(@Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT p FROM Product p
            JOIN FETCH p.applicant
            LEFT JOIN FETCH p.members
            ORDER BY p.id DESC
            """)
    List<Product> findAllForAdmin();
}
